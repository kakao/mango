package com.kakao.mango

import java.lang.Runtime.getRuntime

import com.kakao.mango.concurrent.{NamedExecutors, NamedThreadFactory}
import com.kakao.mango.util.Conversions
import com.stumbleupon.async.{Callback, Deferred}
import com.kakao.shaded.netty.channel.socket.nio.{NioClientSocketChannelFactory, NioWorkerPool}
import com.kakao.shaded.netty.util.HashedWheelTimer

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.language.implicitConversions

package object hbase extends Conversions {

  private[hbase] val EMPTY_BYTE_ARRAY: Array[Byte] = Array[Byte]()

  /** automatically converts HBaseClient's Deferred to Scala [[Future]] */
  implicit def toFuture[T](deferred: Deferred[T]): Future[T] = {
    val promise = Promise[T]()
    deferred.addCallbacks(new Callback[Unit, T] {
      override def call(arg: T): Unit = promise.success(arg)
    }, new Callback[Unit, Throwable] {
      override def call(arg: Throwable): Unit = promise.failure(arg)
    })
    promise.future
  }

  /** an ExecutorService instance to be used by ExecutionContext and Netty's ChannelFactory */
  private[hbase] lazy val executor = NamedExecutors.cached("mango-hbase")

  /** the Netty ChannelFactory to be used for creating HBaseClient */
  private[hbase] lazy val factory = {
    val worker = new NioWorkerPool(executor, getRuntime.availableProcessors() * 2)
    val timer = new HashedWheelTimer(NamedThreadFactory("hashed-wheel-timer"))
    new NioClientSocketChannelFactory(executor, 1, worker, timer)
  }

  /** the ExecutionContext to be used in this package */
  private[hbase] implicit lazy val context: ExecutionContext = ExecutionContext.fromExecutorService(executor)

}
