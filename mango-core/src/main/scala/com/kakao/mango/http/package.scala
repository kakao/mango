package com.kakao.mango

import java.io.InputStream
import java.util.Map.Entry
import java.util.{List => JList}

import com.kakao.mango.concurrent.{NamedExecutors, NamedThreadFactory}
import com.kakao.mango.util.Conversions
import com.kakao.shaded.netty.channel.socket.nio.{NioWorkerPool, NioClientSocketChannelFactory}
import com.kakao.shaded.netty.util.HashedWheelTimer
import com.kakao.shaded.ning.http.client.providers.netty.NettyAsyncHttpProviderConfig
import com.kakao.shaded.ning.http.client.{AsyncHttpClient, AsyncHttpClientConfig, ListenableFuture, Response => NingResponse}

import scala.collection.JavaConversions._
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.language.implicitConversions
import scala.util.Try

/** Provides a simple HTTP client usage, by wrapping AsyncHttpClient.
  *
  * {{{
  *   import com.kakao.mango.concurrent._
  *   import com.kakao.mango.http._
  *
  *   val response = Get("http://example.com").await()
  *   println(response.status)  // prints 200
  *   println(response.body)    // prints "<!doctype html>\n<html>\n ..."
  * }}}
  *
  * A special care has been taken here, to not inadvertently create non-daemon thread,
  * which will cause the program to linger when not destroyed appropriately.
  *
  * This package thus removes the burden of configuring, initializing, and releasing
  * asynchronous HTTP clients from the users, who can just simply use the simple API.
  */
package object http extends Conversions {

  /** ExecutorService used by ExecutionContext and netty's ChannelFactory */
  private[mango] lazy val executor = NamedExecutors.cached("mango-http")

  /** the ExecutionContext to be used by this package */
  private[mango] implicit lazy val context: ExecutionContext = ExecutionContext.fromExecutorService(executor)

  /** the only AsyncHttpClient instance used by this package */
  private[http] val client: AsyncHttpClient = {
    val netty = new NettyAsyncHttpProviderConfig
    val timer = new HashedWheelTimer(NamedThreadFactory("mango-http-timer"))
    val worker = new NioWorkerPool(executor, Runtime.getRuntime.availableProcessors())
    val factory = new NioClientSocketChannelFactory(executor, 1, worker, timer)

    netty.setNettyTimer(timer)
    netty.setBossExecutorService(executor)
    netty.setSocketChannelFactory(factory)

    val config = new AsyncHttpClientConfig.Builder()
      .setAsyncHttpClientProviderConfig(netty)
      .setExecutorService(executor).build()

    new AsyncHttpClient(config)
  }

  private[http] val EMPTY_BYTE_ARRAY: Array[Byte] = Array[Byte]()

  /** automatically converts AsyncHttpClient's Response to [[Response]] */
  implicit def toResponse(response: NingResponse): Response = new Response {
    override def status: Int = response.getStatusCode
    override lazy val headers: Map[String, String] = {
      response.getHeaders.entrySet().flatMap {
        e: Entry[String, JList[String]] => e.getValue.map(v => (e.getKey, v))
      }.toMap
    }
    override def header(name: String): String = headers(name)
    override lazy val body: String = response.getResponseBody("UTF-8")
    override lazy val bytes: Array[Byte] = response.getResponseBodyAsBytes
    override def contentType: String = response.getContentType
    override def stream: InputStream = response.getResponseBodyAsStream
  }

  /** automatically converts AsyncHttpClient's ListenableFuture to [[Future]] */
  implicit def toScalaFuture[T](future: ListenableFuture[T]): Future[T] = {
    val promise = Promise[T]()
    future.addListener(new Runnable {
      override def run(): Unit = promise.complete(Try(future.get()))
    }, executor)
    promise.future
  }

  /** automatically converts ListenableFuture[NingResponse] to Future[Response] */
  implicit def toScalaResponse(future: ListenableFuture[NingResponse]): Future[Response] = toScalaFuture(future).map(toResponse)

}
