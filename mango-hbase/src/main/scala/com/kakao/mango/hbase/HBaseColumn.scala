package com.kakao.mango.hbase

import com.kakao.mango.concurrent.Throttled
import com.kakao.mango.logging.Logging
import com.kakao.shaded.hbase.async._

import scala.collection.JavaConversions._
import scala.concurrent.Future
import scala.util.Try
import scala.util.control.NonFatal

/** This class provides asynchronous access methods to a single HBase column.
  * If a HBase table is being used as a simple key-value storage with fixed column family and qualifier,
  * the methods in this class should be simpler to use than those in [[HBaseTable]].
  *
  * Rather than creating an instance of this class directly, use [[HBase.column]].
  */
class HBaseColumn(table: Array[Byte], family: Array[Byte], qualifier: Array[Byte])(implicit client: HBaseClient) extends Logging {

  client.ensureTableFamilyExists(table, family).onFailure {
    case NonFatal(e) => logger.error("Could not ensure the family exists", e)
  }

  /** make a single PUT request */
  def put(key: String,      value: String     ): Future[Any] = put(key.bytes, value.bytes)

  /** make a single PUT request */
  def put(key: Array[Byte], value: String     ): Future[Any] = put(key, value.bytes)

  /** make a single PUT request */
  def put(key: String,      value: Array[Byte]): Future[Any] = put(key.bytes, value)

  /** make a single PUT request */
  def put(key: Array[Byte], value: Array[Byte]): Future[Any] = {
    client.put(new PutRequest(table, key, family, qualifier, value))
  }

  /** make a single PUT request with a timestamp */
  def put(key: String,      value: String,      timestamp: Long): Future[Any] = put(key.bytes, value.bytes, timestamp)

  /** make a single PUT request with a timestamp */
  def put(key: Array[Byte], value: String,      timestamp: Long): Future[Any] = put(key, value.bytes, timestamp)

  /** make a single PUT request with a timestamp */
  def put(key: String,      value: Array[Byte], timestamp: Long): Future[Any] = put(key.bytes, value, timestamp)

  /** make a single PUT request with a timestamp */
  def put(key: Array[Byte], value: Array[Byte], timestamp: Long): Future[Any] = {
    client.put(new PutRequest(table, key, family, qualifier, value, timestamp))
  }

  /** make bulk PUT requests
    *
    * @param values  a sequence of (row key, value) tuples
    * @param rate    the maximum rate for insertion, in requests per second.
    */
  def putAll(values: Seq[(String, String)], rate: Double = 1e7): Future[Any] = {
    putAllBytes(values.map { case (key, value) => (key.bytes, value.bytes) }, rate)
  }

  /** make bulk PUT requests
    *
    * @param values  a sequence of (row key, value) tuples
    * @param rate    the maximum rate for insertion, in requests per second.
    */
  def putAllBytes(values: Seq[(Array[Byte], Array[Byte])], rate: Double = 1e7): Future[Any] = {
    val futures = for ((key, value) <- Throttled(values, rate)) yield {
      put(key, value)
    }
    Future.sequence(futures).map(_.size)
  }

  /** make a GET request to retrieve the latest cell timestamp and value
    *
    * @param key     the row key
    * @return        an Option that have the cell's timestamp and value, if it exists
    */
  def get(key: String): Future[Option[StringCell]] = get(key.bytes).map(_.map(_.string))

  /** make a GET request to retrieve the latest cell timestamp and value
    *
    * @param key     the row key
    * @return        an Option that have the cell's timestamp and value, if it exists
    */
  def get(key: Array[Byte]): Future[Option[ByteCell]] = getAll(key).map(cells => Try(cells.last).toOption)

  /** make a GET request to retrieve the latest cell timestamp and value
    *
    * @param key     the row key
    * @return        an Option that have the cell's timestamp and value, if it exists
    */
  def getAll(key: String): Future[Seq[StringCell]] = getAll(key.bytes).map(_.map(_.string))

  /** retrieve all cells associated with given row key, not only the latest but all values.
    *
    * @param key     the row key
    * @return        a sequence of cells that contain the timestamps and values.
    */
  def getAll(key: Array[Byte]): Future[Seq[ByteCell]] = {
    client.get(new GetRequest(table, key, family, qualifier)).map(_.map(ByteCell(_)))
  }

  /** retrieve all cells associated with given row key, not only the latest but all values.
    *
    * @param key     the row key
    * @return        a sequence of cells that contain the timestamps and values.
    */
  def delete(key: String): Future[Any] = get(key.bytes)

  /** make a single DELETE request
    *
    * @param key     the row key
    * @return        a Future that finishes when the operation is done.
    */
  def delete(key: Array[Byte]): Future[Any] = {
    client.delete(new DeleteRequest(table, key, family, qualifier))
  }


  /** Scan the table and return all cells that satisfies the condition,
    * assuming that the row key, column qualifier, and value are all UTF-8 strings.
    * All parameters are optional, and the conditions will not be applied where the parameter is not given.
    *
    * @param startKey    the row key to start scanning
    * @param stopKey     the row key to stop scanning
    * @param regexp      the regular expression that the row key as a UTF-8 string has to satisfy
    * @param limit       the maximum number of rows to return
    * @param filter      an instance of ScanFilter to apply, for more sophiscated filtering
    * @return            a sequence of (matched row key, corresponding cells) containing the result
    */
  def scan( startKey  : String = "",
            stopKey   : String = "",
            regexp    : String = "",
            limit     : Int    = 100,
            filter    : ScanFilter = null): Future[Seq[(String, Seq[StringCell])]] = {

    scanBytes(startKey.bytes, stopKey.bytes, regexp, limit, filter).map {
      rows => rows.map {
        case (key, cells) => (key.string, cells.map(_.string))
      }
    }
  }

  /** Scan the table and return all cells that satisfies the condition,
    * assuming that the row key, column qualifier, and value are all UTF-8 strings.
    * All parameters are optional, and the conditions will not be applied where the parameter is not given.
    *
    * @param startKey    the row key to start scanning
    * @param stopKey     the row key to stop scanning
    * @param regexp      the regular expression that the row key as a UTF-8 string has to satisfy
    * @param limit       the maximum number of rows to return
    * @param filter      an instance of ScanFilter to apply, for more sophiscated filtering
    * @return            a sequence of (matched row key, corresponding cells) containing the result
    */
  def scanBytes( startKey  : Array[Byte] = EMPTY_BYTE_ARRAY,
                 stopKey   : Array[Byte] = EMPTY_BYTE_ARRAY,
                 regexp    : String      = "",
                 limit     : Int         = 100,
                 filter    : ScanFilter  = null): Future[Seq[(Array[Byte], Seq[ByteCell])]] = {

    val scanner = client.newScanner(table)

    if (startKey.nonEmpty)  scanner.setStartKey(startKey)
    if (stopKey.nonEmpty)   scanner.setStopKey(stopKey)
    if (filter != null)     scanner.setFilter(filter)

    scanner.setFamily(family)
    scanner.setQualifier(qualifier)

    val result = scanner.nextRows(limit).map {
      case null => Seq()
      case rows => rows.map {
        cells => (cells.head.key(), cells.map { kv => ByteCell(kv) })
      }
    }

    result.onComplete {
      _ => scanner.close()
    }

    result
  }


}
