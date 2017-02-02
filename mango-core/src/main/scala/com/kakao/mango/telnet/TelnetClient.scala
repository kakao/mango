package com.kakao.mango.telnet

import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import java.util.concurrent.{ArrayBlockingQueue, ConcurrentLinkedQueue}
import java.util.concurrent.atomic.AtomicInteger

import com.kakao.mango.concurrent.{NamedThreadFactory, NamedExecutors}
import com.kakao.mango.telnet.TelnetClient._
import com.kakao.shaded.netty.bootstrap.ClientBootstrap
import com.kakao.shaded.netty.channel._
import com.kakao.shaded.netty.channel.socket.nio.{NioWorkerPool, NioClientSocketChannelFactory}
import com.kakao.shaded.netty.handler.codec.frame.{DelimiterBasedFrameDecoder, Delimiters}
import com.kakao.shaded.netty.handler.codec.string.{StringDecoder, StringEncoder}
import com.kakao.shaded.netty.util.HashedWheelTimer
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.language.{implicitConversions, postfixOps}
import scala.util.{Failure, Random, Success}

object TelnetClient {
  lazy val executor = NamedExecutors.cached("mango-telnet")
  lazy val retry = NamedExecutors.scheduled("mango-telnet-retry")
  implicit lazy val context = ExecutionContext.fromExecutor(executor)
  lazy val timer = new HashedWheelTimer(NamedThreadFactory("mango-telnet-timer"))

  val logger = LoggerFactory.getLogger(classOf[TelnetClient])

  /** automatically converts [[ChannelFuture]] to Scala's [[Future]] */
  implicit def convert(future: ChannelFuture): Future[Channel] = {
    val promise = Promise[Channel]()
    future.addListener(new ChannelFutureListener {
      override def operationComplete(future: ChannelFuture): Unit = {
        if (future.isSuccess) {
          promise.success(future.getChannel)
        } else {
          promise.failure(future.getCause)
        }
      }
    })
    promise.future
  }
}

/** A telnet client that connects to any of given hosts, and reconnects when disconnected */
class TelnetClient(hosts: Seq[String], port: Int) extends SimpleChannelUpstreamHandler {
  require(hosts.nonEmpty)

  def this(host: String, port: Int) = this(Seq(host), port)

  val index = new AtomicInteger(Random.nextInt(hosts.size))

  @volatile var channel: Channel = _
  @volatile var listener: String => Unit = str => ()
  @volatile var closed = false

  def onMessage(listener: String => Unit): Unit = {
    this.listener = listener
    if (channel == null) connect()
  }

  private def connect(): Future[Channel] = {
    channel = null

    val host = hosts(index.getAndIncrement() % hosts.size)
    logger.info(s"connecting to $host:$port")

    val workers = new NioWorkerPool(executor, 1)
    val factory = new NioClientSocketChannelFactory(executor, 1, workers, timer)
    val bootstrap = new ClientBootstrap(factory)
    bootstrap.setPipelineFactory(new ChannelPipelineFactory {
      override def getPipeline: ChannelPipeline = {
        val pipeline = Channels.pipeline()
        pipeline.addLast("framer", new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter(): _*))
        pipeline.addLast("decoder", new StringDecoder(StandardCharsets.UTF_8))
        pipeline.addLast("encoder", new StringEncoder(StandardCharsets.UTF_8))
        pipeline.addLast("handler", TelnetClient.this)
        pipeline
      }
    })
    bootstrap.setOption("tcpNoDelay", true)
    bootstrap.setOption("sendBufferSize", 1048576)

    val future = bootstrap.connect(new InetSocketAddress(host, port))

    future.onComplete {
      case Success(c) =>
        logger.info(s"Connected to $c")
        channel = c
        if (!outbox.isEmpty) {
          logger.warn(s"replaying ${outbox.size()} messages")
          while (!outbox.isEmpty) {
            send(outbox.take)
          }
        }
      case Failure(e) =>
        logger.error("Could not connect to host; will retry in a second", e)
        retry.scheduleIn(1 second) {
          connect()
        }
    }

    future
  }

  def close(): Future[Any] = {
    closed = true
    Option(channel) match {
      case Some(c) => c.close()
      case None => Future.successful(())
    }
  }

  private val outbox = new ArrayBlockingQueue[String](100000)

  def send(line: String): Unit = {
    Option(channel) match {
      case Some(c) =>
        channel.write(line + "\n")
      case None =>
        if (!outbox.offer(line)) {
          logger.warn(s"the message $line is because the retry queue was full")
        }
    }
  }

  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent): Unit = {
    val message = e.getMessage.asInstanceOf[String]
    listener(message)
  }

  override def handleUpstream(ctx: ChannelHandlerContext, e: ChannelEvent): Unit = {
    if (e.isInstanceOf[ChannelStateEvent]) {
      logger.info(s"Channel state event: $e")
    }
    super.handleUpstream(ctx, e)
  }

  override def channelDisconnected(ctx: ChannelHandlerContext, e: ChannelStateEvent): Unit = {
    channel = null

    if (!closed) {
      logger.error("Channel disconnected abruptly; reconnecting...")
      retry.scheduleIn(1 second) {
        connect()
      }
    }
  }
}
