package com.kakao.mango

import java.util.concurrent.atomic.{AtomicBoolean, AtomicInteger}
import java.util.{List => JList}

import rx.functions.Func1
import rx.{Observer, Observable}

import scala.concurrent.{Promise, Future}
import scala.language.implicitConversions
import scala.util.Try

package object couchbase {

  /** Convert an [[Observable]] that Coucchbase Java API returns to a Scala [[Future]].
    * It assumes that the [[Observable]] will have only one entry, and if it doesn't
    * it should be converted to such one first, using toList() or last().
    */
  private[couchbase] implicit def toScalaFuture[T](observable: Observable[T]): Future[Option[T]] = {
    val promise = Promise[Option[T]]()
    observable.toList.doOnEach(new Observer[JList[T]] {
      val completed = new AtomicBoolean(false)
      def complete(fn: => Unit): Unit = if (completed.compareAndSet(false, true)) fn
      override def onCompleted(): Unit = complete { promise.success(None) }
      override def onError(e: Throwable): Unit = complete { promise.failure(e) }
      override def onNext(t: JList[T]): Unit = complete { promise.success(Try(t.get(0)).toOption) }
    }).subscribe()
    promise.future
  }

  /** automatically converts a Scala [[Function1]] to Rx's [[Func1]] */
  private[couchbase] def fn[T, R](func: T => R) = new Func1[T, R] {
    override def call(t: T): R = func(t)
  }

}
