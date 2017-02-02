package com.kakao.mango.concurrent

import java.util.concurrent.{ConcurrentMap, TimeUnit, TimeoutException}

import com.kakao.shaded.netty.util.{HashedWheelTimer, Timeout, TimerTask}

import scala.collection.JavaConversions._
import scala.concurrent.duration._
import scala.concurrent.{Future, Promise}
import scala.language.implicitConversions

/** A trait that contains implicit converters that enable the extension methods in this package */
trait ConcurrentConverters {

  /** Uses Netty's timer implemention which is efficient than that of ScheduledExecutor in Java */
  lazy val timer = new HashedWheelTimer(NamedThreadFactory("mango-timer"))

  /** Returns a timeout that fails after the given timeout.  */
  def timeout(duration: Duration): Future[Nothing] = {
    val promise = Promise[Nothing]()
    timer.newTimeout(new TimerTask {
      override def run(timeout: Timeout): Unit = {
        promise.failure(new TimeoutException(s"Operation was timed out after $duration"))
      }
    }, duration.toMillis, TimeUnit.MILLISECONDS)
    promise.future
  }

  implicit def toRichFuture[T](future: Future[T])(implicit timeout: Duration = 5.seconds): RichFuture[T] = new RichFuture[T](future, timeout)
  implicit def toEnsuring[K, V](map: ConcurrentMap[K, V]): EnsuringMap[K, V] = new EnsuringMap(map)
  implicit def toEnsuring[K, V](map: scala.collection.concurrent.Map[K, V]): EnsuringMap[K, V] = new EnsuringMap(map)

}
