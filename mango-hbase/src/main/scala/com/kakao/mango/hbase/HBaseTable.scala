package com.kakao.mango.hbase

import java.nio.charset.StandardCharsets

import com.kakao.mango.concurrent.Throttled
import com.kakao.mango.logging.Logging
import com.kakao.shaded.hbase.async._

import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.concurrent.Future
import scala.util.control.NonFatal

/** This class provides asynchronous access methods to an HBase table.
  * Rather than creating an instance of this class directly, use [[HBase.table]]
  */
class HBaseTable(val name: String)(implicit client: HBaseClient) extends Logging {

  val table = name.bytes

  client.ensureTableExists(table).onFailure {
    case NonFatal(e) => logger.error("Could not ensure that the table exists", e)
  }

  /** Returns an [[HBaseColumn]] instance specified by given family and qualifier */
  def column(family: String, qualifier: String): HBaseColumn = new HBaseColumn(table, family.bytes, qualifier.bytes)

  /** Returns an [[HBaseColumn]] instance specified by given family and qualifier */
  def column(family: String, qualifier: Array[Byte]): HBaseColumn = new HBaseColumn(table, family.bytes, qualifier)

  /** Returns an [[HBaseColumn]] instance specified by given family and qualifier */
  def column(family: Array[Byte], qualifier: Array[Byte]): HBaseColumn = new HBaseColumn(table, family, qualifier)

  /** Returns an [[HBaseColumn]] instance specified by given String of format "family:qualifier" */
  def column(column: String): HBaseColumn = {
    column.split(":") match {
      case Array(family, qualifier) => new HBaseColumn(table, family.bytes, qualifier.bytes)
      case _ => throw new RuntimeException(s"invalid column name $column")
    }
  }

  /** perform a PUT request
    *
    * @param key         row key
    * @param family      column family
    * @param qualifier   column qualifier
    * @param value       value
    * @return            a Future that finishes when the operation is done.
    */
  def put(key: String, family: String, qualifier: String, value: String): Future[Any] = {
    put(key.bytes, family.bytes, qualifier.bytes, value.bytes)
  }

  /** perform a PUT request
    *
    * @param key         row key
    * @param family      column family
    * @param qualifier   column qualifier
    * @param value       value
    * @return            a Future that finishes when the operation is done.
    */
  def put(key: Array[Byte], family: Array[Byte], qualifier: Array[Byte], value: Array[Byte]): Future[Any] = {
    client.put(new PutRequest(table, key, family, qualifier, value))
  }

  /** Perform a bulk PUT request
    *
    * @param family  column family
    * @param values  A sequence of (row key, column qualifier, value) tuples
    * @param rate    the maximum rate for insertion, in requests per second.
    */
  def putAll(family: String, values: Seq[(String, String, String)], rate: Double = 1e7): Future[Any] = {
    putAllBytes(family, values.map { case (key, qualifier, value) => (key.bytes, qualifier.bytes, value.bytes) }, rate)
  }

  /** Perform a bulk PUT request
    *
    * @param family  column family
    * @param values  A sequence of (row key, column qualifier, value) tuples
    * @param rate    the maximum rate for insertion, in requests per second.
    */
  def putAllBytes(family: String, values: Seq[(Array[Byte], Array[Byte], Array[Byte])], rate: Double = 1e7): Future[Any] = {
    val familyBytes = family.bytes
    val futures = for ((key, qualifier, value) <- Throttled(values, rate)) yield {
      put(key, familyBytes, qualifier, value)
    }
    Future.sequence(futures).map(_.size)
  }

  /** Retrieve all Cells corresponding to given row key,
    * assuming that the row key, column qualifier, and value are all UTF-8 strings
    *
    * @param key   the row key, in a String.
    * @return      a sequence of cells that has the latest timestamps in each column.
    */
  def get(key: String): Future[Seq[(StringColumn, StringCell)]] = get(key.bytes).map(_.map {
    case (column, cell) => (column.string, cell.string)
  })

  /** Retrieve all Cells corresponding to given row key.
    *
    * @param key   the row key, in a byte array.
    * @return      a sequence of cells that has the latest timestamps in each column.
    */
  def get(key: Array[Byte]): Future[Seq[(ByteColumn, ByteCell)]] = {
    getAll(key).map {
      _.foldLeft(mutable.LinkedHashMap[ByteColumn, ByteCell]()) {
        case (map, (column, cell)) => map.update(column, cell); map
      }.toSeq
    }
  }

  /** Retrieve all Cells corresponding to given row key and column family,
    * assuming that the row key, column qualifier, and value are all UTF-8 strings.
    *
    * @param key   the row key, in a String
    * @return      a sequence of cells that has the latest timestamps in each column.
    */
  def get(key: String, family: String): Future[Seq[(StringColumn, StringCell)]] = {
    get(key.bytes, family.bytes).map(_.map {
      case (column, cell) => (column.string, cell.string)
    })
  }

  /** Retrieve all Cells corresponding to given row key and column family,
    *
    * @param key   the row key, in a String
    * @return      a sequence of cells that has the latest timestamps in each column.
    */
  def get(key: Array[Byte], family: Array[Byte]): Future[Seq[(ByteColumn, ByteCell)]] = {
    getAll(key, family).map {
      _.foldLeft(mutable.LinkedHashMap[ByteColumn, ByteCell]()) {
        case (map, (column, cell)) => map.update(column, cell); map
      }.toSeq
    }
  }

  /** Retrieve a Cell corresponding to given row key, column family, and qualifier,
    * assuming that the row key, column qualifier, and value are all UTF-8 strings.
    *
    * @param key         the row key, in a String
    * @param family      the column Family
    * @param qualifier   the column Qualifier
    * @return            a [[StringCell]] instance, or None.
    */
  def get(key: String, family: String, qualifier: String): Future[Option[StringCell]] = {
    getAll(key, family, qualifier).map {
      case seq if seq.isEmpty => None
      case seq => Some(seq.last)
    }
  }

  /** Retrieve a Cell corresponding to given row key, column family, and qualifier.
    *
    * @param key         the row key, in a String
    * @param family      the column Family
    * @param qualifier   the column Qualifier
    * @return            a [[ByteCell]] instance, or None.
    */
  def get(key: Array[Byte], family: Array[Byte], qualifier: Array[Byte]): Future[Option[ByteCell]] = {
    getAll(key, family, qualifier).map {
      case seq if seq.isEmpty => None
      case seq => Some(seq.last)
    }
  }

  /** Retrieve all Cells corresponding to a row key, along with the timestamps,
    * assuming that the row key, column qualifier, and value are all UTF-8 strings.
    *
    * @param key   the row key, in a String
    * @return      a Sequence containing (family, qualifier) -> (timestamp, value) entries
    */
  def getAll(key: String): Future[Seq[(StringColumn, StringCell)]] = getAll(key.bytes).map(_.map {
    case (column, cell) => (column.string, cell.string)
  })

  /** Retrieve all Cells corresponding to a row key, along with the timestamps.
    *
    * @param key   the row key, in a String
    * @return      a Sequence containing (family, qualifier) -> (timestamp, value) entries
    */
  def getAll(key: Array[Byte]): Future[Seq[(ByteColumn, ByteCell)]] = {
    client.get(new GetRequest(table, key)).map {
      _.map(kv => (ByteColumn(kv), ByteCell(kv)))
    }
  }

  /** Retrieve all Cells corresponding to a row key and column family, along with the timestamps,
    * assuming that the row key, column qualifier, and value are all UTF-8 strings.
    *
    * @param key   the row key, in a String
    * @return      a Sequence containing (family, qualifier) -> (timestamp, value) entries
    */
  def getAll(key: String, family: String): Future[Seq[(StringColumn, StringCell)]] = {
    getAll(key.bytes, family.bytes).map(_.map {
      case (column, cell) => (column.string, cell.string)
    })
  }

  /** Retrieve all Cells corresponding to a row key and column family, along with the timestamps.
    *
    * @param key   the row key, in a String
    * @return      a Sequence containing (family, qualifier) -> (timestamp, value) entries
    */
  def getAll(key: Array[Byte], family: Array[Byte]): Future[Seq[(ByteColumn, ByteCell)]] = {
    client.get(new GetRequest(table, key, family)).map {
      _.map(kv => (ByteColumn(kv), ByteCell(kv)))
    }
  }

  /** Retrieve all Cells corresponding to a row key, column family and qualifier, along with the timestamps,
    * assuming that the row key, column qualifier, and value are all UTF-8 strings.
    *
    * @param key   the row key, in a String
    * @return      a Sequence containing (timestamp, value) entries
    */
  def getAll(key: String, family: String, qualifier: String): Future[Seq[StringCell]] = {
    getAll(key.bytes, family.bytes, qualifier.bytes).map { _.map(_.string) }
  }

  /** Retrieve all Cells corresponding to a row key, column family and qualifier, along with the timestamps.
    *
    * @param key   the row key, in a String
    * @return      a Sequence containing (timestamp, value) entries
    */
  def getAll(key: Array[Byte], family: Array[Byte], qualifier: Array[Byte]): Future[Seq[ByteCell]] = {
    client.get(new GetRequest(table, key, family, qualifier)).map { _.map(ByteCell(_)) }
  }

  /** Scan the table and return all cells that satisfies the condition,
    * assuming that the row key, column qualifier, and value are all UTF-8 strings.
    * All parameters are optional, and the conditions will not be applied where the parameter is not given.
    *
    * @param startKey    the row key to start scanning
    * @param stopKey     the row key to stop scanning
    * @param regexp      the regular expression that the row key as a UTF-8 string has to satisfy
    * @param family      the column family to scan
    * @param qualifier   the column qualifier to scan
    * @param limit       the maximum number of rows to return
    * @param filter      an instance of ScanFilter to apply, for more sophiscated filtering
    * @return            a sequence of (matched row key, corresponding cells) containing the result
    */
  def scan( startKey  : String = "",
            stopKey   : String = "",
            regexp    : String = "",
            family    : String = "",
            qualifier : String = "",
            limit     : Int    = 100,
            filter    : ScanFilter = null ): Future[Seq[(String, Seq[(StringColumn, StringCell)])]] = {

    scanBytes(startKey.bytes, stopKey.bytes, regexp, family.bytes, qualifier.bytes, limit, filter).map {
      rows => rows.map {
        case (key, cells) => (key.string, cells.map {
          case (column, cell) => (column.string, cell.string)
        })
      }
    }
  }

  /** Scan the table and return all cells that satisfies the condition.
    * All parameters are optional, and the conditions will not be applied where the parameter is not given.
    *
    * @param startKey    the row key to start scanning
    * @param stopKey     the row key to stop scanning
    * @param regexp      the regular expression that the row key as a UTF-8 string has to satisfy
    * @param family      the column family to scan
    * @param qualifier   the column qualifier to scan
    * @param limit       the maximum number of rows to return
    * @param filter      an instance of ScanFilter to apply, for more sophiscated filtering
    * @return            a sequence of (matched row key, corresponding cells) containing the result
    */
  def scanBytes( startKey  : Array[Byte] = EMPTY_BYTE_ARRAY,
                 stopKey   : Array[Byte] = EMPTY_BYTE_ARRAY,
                 regexp    : String = "",
                 family    : Array[Byte] = EMPTY_BYTE_ARRAY,
                 qualifier : Array[Byte] = EMPTY_BYTE_ARRAY,
                 limit     : Int         = 100,
                 filter    : ScanFilter  = null ): Future[Seq[(Array[Byte], Seq[(ByteColumn, ByteCell)])]] = {

    val scanner = client.newScanner(table)

    if (startKey.nonEmpty)  scanner.setStartKey(startKey)
    if (stopKey.nonEmpty)   scanner.setStopKey(stopKey)
    if (regexp.nonEmpty)    scanner.setKeyRegexp(regexp, StandardCharsets.UTF_8)
    if (family.nonEmpty)    scanner.setFamily(family)
    if (qualifier.nonEmpty) scanner.setQualifier(qualifier)
    if (filter != null)     scanner.setFilter(filter)

    val result = scanner.nextRows(limit).map {
      case null => Seq()
      case rows => rows.map {
        cells => (cells.head.key(), cells.map { kv => (ByteColumn(kv), ByteCell(kv)) })
      }
    }

    result.onComplete {
      _ => scanner.close()
    }

    result
  }

  /** Atomically increase a Long value stored in an HBase Cell.
    *
    * @param key        the row key
    * @param family     the column family
    * @param qualifier  the column qualifier
    * @param amount     the amount to increase
    * @return
    */
  def atomicIncrement(key: String, family: String, qualifier: String, amount: Long): Future[Long] = {
    client.atomicIncrement(new AtomicIncrementRequest(table.string, key, family, qualifier, amount)).map(_.toLong)
  }

  /** Retrieves the timestamp and the Long value stored in an HBase Cell.
    *
    * @param key        the row key
    * @param family     the column family
    * @param qualifier  the amount to increase
    * @return
    */
  def getCounter(key: String, family: String, qualifier: String): Future[Option[CounterCell]] = {
    get(key.bytes, family.bytes, qualifier.bytes).map {
      case Some(value) => Some(CounterCell(value))
      case None => None
    }
  }

  /** Atomically increase a Long value stored in an HBase Cell, using Asynchbase's buffering feature.
    * For a very frequent updates, this will decrease the overhead on both the client and the server.
    *
    * @param key        the row key
    * @param family     the column family
    * @param qualifier  the column qualifier
    * @param amount     the amount to increase
    * @return
    */
  def bufferedIncrement(key: String, family: String, qualifier: String, amount: Long): Future[Long] = {
    client.bufferAtomicIncrement(new AtomicIncrementRequest(table.string, key, family, qualifier, amount)).map(_.toLong)
  }
}
