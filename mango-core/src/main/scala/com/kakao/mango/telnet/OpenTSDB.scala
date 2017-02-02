package com.kakao.mango.telnet

import java.net.InetAddress

import org.slf4j.LoggerFactory
import OpenTSDB._

object OpenTSDB {
  val logger = LoggerFactory.getLogger(classOf[OpenTSDB])
  val localhost = InetAddress.getLocalHost.getHostName
}

/** A client for sending metrics to OpenTSDB via its telnet API.
  * This should have much lower overhead than its HTTP API.
  */
class OpenTSDB(hosts: Seq[String], port: Int = 4242, includeLocalHost: Boolean = true) {

  def this(hosts: String*) = this(hosts)

  private val client = new TelnetClient(hosts, port)

  client.onMessage { message =>
    logger.warn(s"Response from OpenTSDB: $message")
  }

  def stats(): Unit = {
    client.send("stats")
  }

  def version(): Unit = {
    client.send("version")
  }

  def put(metric: String, value: AnyVal, tags: (String, Any)*): Unit = {
    val timestamp = System.currentTimeMillis()
    put(timestamp, metric, value, tags: _*)
  }

  def put(timestamp: Long, metric: String, value: AnyVal, tags: (String, Any)*): Unit = {
    val now = (timestamp / 1000.0).formatted("%.3f")
    val tagline = tags.map { case (k, v) => s"$k=$v" }.mkString(" ")
    val line = s"put $metric $now $value ${if (includeLocalHost) s"host=$localhost " else ""}$tagline"
    client.send(line)
  }

  def close() = client.close()

}
