package com.kakao.mango.hbase

import java.io.IOException
import java.net.{ServerSocket, Socket}

import com.kakao.mango.concurrent.ConcurrentConverters
import com.kakao.mango.logging.LogLevelOverrider
import com.kakao.mango.zk.TestZooKeeper
import com.kakao.shaded.guava.io.Files
import com.kakao.shaded.hbase.async.TableNotFoundException
import org.apache.hadoop.hbase._
import org.apache.hadoop.hbase.client.ConnectionFactory
import org.apache.hadoop.hbase.master.HMaster
import org.apache.hadoop.hbase.regionserver.HRegionServer
import org.scalatest.Suite

import scala.concurrent.{Future, TimeoutException}

trait TestHBase extends TestZooKeeper with ConcurrentConverters { this: Suite =>

  var hbaseCluster: LocalHBaseCluster = _
  var hbase: HBase = _

  override protected def beforeAll(): Unit = {
    // launch zookeeper first
    super.beforeAll()

    logger.info("Preparing for HBase tests; ignore any error logs complaining about the -ROOT- region")

    for (port <- Seq(16010)) {
      try {
        val socket = new ServerSocket(port)
        socket.close()
      } catch {
        case e: IOException =>
          throw new RuntimeException(s"TCP port $port is required for tests but not available")
      }
    }

    // launch a standalone HBase server
    val root = Files.createTempDir().getAbsolutePath

    val conf = HBaseConfiguration.create()
    conf.set("proc_master", "")
    conf.set("hbase.rootdir", root)
    conf.set("hbase.home.dir", root)
    conf.set("hbase.log.dir", root)
    conf.set("hbase.log.file", "hbase.log")
    conf.set("hbase.zookeeper.property.dataDir", root + "/zookeeper")
    conf.set("hbase.id.str", "mango-hbase-test")
    conf.set("hbase.root.logger", "INFO,RFA")

    // turn off noisy logs coming from the server
    LogLevelOverrider.set(
      "org.apache.zookeeper" -> "ERROR",
      "org.apache.hadoop" -> "ERROR",
      "SecurityLogger.org.apache.hadoop.hbase" -> "ERROR",
      "org.mortbay" -> "ERROR"
    )
    hbaseCluster = new LocalHBaseCluster(conf, 1, 1, classOf[HMaster], classOf[HRegionServer])
    hbaseCluster.startup()

    // wait until port 16010 becomes available
    var connected = false
    while (!connected) {
      logger.info("Waiting for HBase server to launch...")
      try {
        val socket = new Socket("localhost", 16010)
        logger.info("HBase server is available")
        socket.close()

        hbase = HBase("localhost")
        connected = true
      } catch {
        case _: IOException => Thread.sleep(1000) // retry
      }
    }

    // wait until the HMaster is actually ready
    var ready = false
    while (!ready) {
      try {
        val future: Future[_] = hbase.client.ensureTableExists("test")
        future.sync()
        ready = true
      } catch {
        case e @ (_: TimeoutException | _: PleaseHoldException) =>
          logger.info(s"Waiting for HMaster to be ready... (skipping ${e.getClass.getSimpleName})")
          Thread.sleep(1000)
        case _: TableNotFoundException =>
          // even if table is not there, the server is ready
          ready = true
        case e: Throwable =>
          logger.error("Unexpected error from HBase", e)
      }
    }

    logger.info("HMaster is now ready")

    // create table "test" and column family "family" for testing
    val connection = ConnectionFactory.createConnection()
    val admin = connection.getAdmin
    val table = new HTableDescriptor(TableName.valueOf("test"))
    val family = new HColumnDescriptor("family")
    table.addFamily(family)
    admin.createTable(table)
  }

  override protected def afterAll(): Unit = {
    hbase.close().sync()

    LogLevelOverrider.off("org.apache.hadoop.hbase.master.HMasterCommandLine")
    logger.info("Shutting down HBase...")
    hbaseCluster.shutdown()
    logger.info("HBase server terminated")

    // close ZooKeeper
    super.afterAll()
  }
}
