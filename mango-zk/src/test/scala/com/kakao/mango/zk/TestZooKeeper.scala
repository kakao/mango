package com.kakao.mango.zk

import java.io.{File, IOException}
import java.net.{ServerSocket, Socket}
import java.util.concurrent.TimeUnit

import com.kakao.mango.concurrent.NamedExecutors
import com.kakao.mango.logging.{LogLevelOverrider, Logging}
import com.kakao.shaded.guava.io.Files
import org.apache.zookeeper.server.persistence.FileTxnSnapLog
import org.apache.zookeeper.server.{ServerCnxnFactory, ServerConfig, ZooKeeperServer}
import org.scalatest.{BeforeAndAfterAll, Suite}

trait TestZooKeeper extends BeforeAndAfterAll with Logging { this: Suite =>

  /** the TCP port to run the test server */
  val zkServerPort = 2181
  val zkServerExecutor = NamedExecutors.single("zookeeper-server")
  var zk: ZooKeeperConnection = _

  override protected def beforeAll(): Unit = {
    logger.info("Launching a standalone ZooKeeper server for testing...")

    try {
      val socket = new ServerSocket(zkServerPort)
      socket.close()
    } catch {
      case e: IOException =>
        throw new RuntimeException(s"TCP port $zkServerPort is required for tests but not available")
    }

    zkServerExecutor.submit {
      LogLevelOverrider.error("org.apache.zookeeper")

      val datadir = Files.createTempDir().getAbsolutePath
      val config = new ServerConfig
      config.parse(Array(zkServerPort.toString, datadir))

      val zkServer = new ZooKeeperServer
      zkServer.setTxnLogFactory(new FileTxnSnapLog(new File(datadir), new File(datadir)))
      zkServer.setTickTime(6000)
      zkServer.setMinSessionTimeout(6000)
      zkServer.setMaxSessionTimeout(6000)

      val cnxnFactory = ServerCnxnFactory.createFactory

      try {
        cnxnFactory.configure(config.getClientPortAddress, 60)
        cnxnFactory.startup(zkServer)
        cnxnFactory.join()
      } catch {
        case _: InterruptedException =>
          logger.info("ZooKeeper server interrupted; shutting down...")
          cnxnFactory.shutdown()
          cnxnFactory.join()
          if (zkServer.isRunning) {
            zkServer.shutdown()
          }
          logger.info("ZooKeeper server stopped")
      }
    }

    var connected = false
    while (!connected) {
      logger.info("Waiting for ZooKeeper server to launch...")
      try {
        val socket = new Socket("localhost", zkServerPort)
        logger.info("ZooKeeper server is available")
        socket.close()

        zk = ZooKeeperConnection(s"localhost:$zkServerPort")
        connected = true
      } catch {
        case _: IOException => Thread.sleep(1000) // retry
      }
    }

    super.beforeAll()
  }

  override protected def afterAll(): Unit = {
    try super.afterAll()
    finally {
      zk.close()
      logger.info("Interrupting ZooKeeper server...")
      zkServerExecutor.shutdownNow()
      while (!zkServerExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
        logger.info("awaiting ZooKeeper server termination...")
      }
      logger.info("ZooKeeper server terminated")
    }
  }
}
