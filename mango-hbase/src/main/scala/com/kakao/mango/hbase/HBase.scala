package com.kakao.mango.hbase

import com.kakao.mango.concurrent.{KeyedSingletons, KeyedSingletons$}
import com.kakao.shaded.hbase.async.{AtomicIncrementRequest, GetRequest, PutRequest, HBaseClient}

import scala.concurrent.Future
import scala.collection.JavaConversions._

/** The entry point of mango-hbase.
  * This object manages singltones that are keyed by the tuple of the quorum address and the base path,
  * so users can call {{{HBase("zookeeper address")}}} multiple times without creating multiple clients.
  */
object HBase extends KeyedSingletons[(String, String), HBase] {

  private[HBase] def remove(quorum: String, base: String) = registry.remove((quorum, base))

  override def newInstance(key: (String, String)): HBase = key match {
    case (quorum, base) => new HBase(quorum, base)
  }

  def apply(quorum: String, base: String = "/hbase"): HBase = apply((quorum, base))

}

/** This class represents a client connection to an HBase cluster.
  *
  * @param quorum   the ZooKeeper connectString to the HBase quorum
  * @param base     the ZooKeeper path to be used as the base path, usually "/hbase"
  */
class HBase(quorum: String, base: String) {

  implicit val client = new HBaseClient(quorum, base, factory)

  /** return an [[HBaseTable]] instance for accessing an HBase table */
  def table(name: String): HBaseTable = new HBaseTable(name)

  /** return an [[HBaseColumn]] instance for accessing an HBase column */
  def column(name: String, family: String, qualifier: String) = new HBaseColumn(name.bytes, family.bytes, qualifier.bytes)

  /** return an [[HBaseColumn]] instance for accessing an HBase column */
  def column(name: String, family: String, qualifier: Array[Byte]) = new HBaseColumn(name.bytes, family.bytes, qualifier)

  /** close this client */
  def close(): Future[Any] = {
    if (HBase.remove(quorum, base) == this) {
      client.shutdown()
    } else {
      Future.failed(new RuntimeException(s"HBase client to $quorum$base has been already closed"))
    }
  }

  /** test if this client connection is alive, by sending an empty GET request.
    *
    * @param table   the table to be used for testing, "ping" by default.
    * @return        A future that is successful if the connection is alive.
    */
  def ping(table: String = "ping"): Future[Any] = {
    client.ensureTableExists(table)
  }

  /** make a simple PUT request with Strings */
  def put(table: String, key: String, family: String, qualifier: String, value: String): Future[Any] = {
    client.put(new PutRequest(table, key, family, qualifier, value))
  }

  /** make a simple PUT request with byte arrays */
  def put(table: String, key: Array[Byte], family: Array[Byte], qualifier: Array[Byte], value: Array[Byte]): Future[Any] = {
    client.put(new PutRequest(table.bytes, key, family, qualifier, value))
  }

  /** make a simple GET request with Strings, to retrieve the latest cell only */
  def get(table: String, key: String, family: String, qualifier: String): Future[Option[StringCell]] = {
    getAll(table, key, family, qualifier).map {
      case array if array.isEmpty => None
      case array => Some(array.last)
    }
  }

  /** make a simple GET request with Strings, for all timestamps, not just latest cells */
  def getAll(table: String, key: String, family: String, qualifier: String): Future[Seq[StringCell]] = {
    client.get(new GetRequest(table, key, family, qualifier)).map(_.map(StringCell(_)))
  }

  /** make a simple GET request with byte arrays, to retrieve the latest cell only */
  def get(table: String, key: Array[Byte], family: Array[Byte], qualifier: Array[Byte]): Future[Option[ByteCell]] = {
    getAll(table, key, family, qualifier).map {
      case array if array.isEmpty => None
      case array => Some(array.last)
    }
  }

  /** make a simple GET request with byte arrays, for all timestamps, not just latest cells */
  def getAll(table: String, key: Array[Byte], family: Array[Byte], qualifier: Array[Byte]): Future[Seq[ByteCell]] = {
    client.get(new GetRequest(table.bytes, key, family, qualifier)).map(_.map(ByteCell(_)))
  }

  /** make an atomic increment request */
  def atomicIncrement(table: String, key: String, family: String, qualifier: String, amount: Long): Future[Long] = {
    client.atomicIncrement(new AtomicIncrementRequest(table, key, family, qualifier, amount)).map(_.toLong)
  }

  /** retrieve the value of a counter cell */
  def getCounter(table: String, key: String, family: String, qualifier: String): Future[Option[CounterCell]] = {
    get(table, key.bytes, family.bytes, qualifier.bytes).map {
      case Some(value) => Some(CounterCell(value))
      case None => None
    }
  }

  /** make a buffered atomic increment request */
  def bufferedIncrement(table: String, key: String, family: String, qualifier: String, amount: Long): Future[Long] = {
    client.bufferAtomicIncrement(new AtomicIncrementRequest(table, key, family, qualifier, amount)).map(_.toLong)
  }
}
