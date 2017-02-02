package com.kakao.mango.concurrent

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Await, Future}
import scala.language.implicitConversions

/**
  * a utility to block on scala Future in an easier way
  * provides methods await(), get(), block(), and sync(), which do the same thing.
  * Easier to read than using Await.result(), when used in tests.
  *
  * {{{
  * import com.kakao.mango.concurrent._
  *
  * val future: Future[T] = someAsyncMethod()
  * val result: T = future.get()
  * }}}
  */
class RichFuture[T](val future: Future[T], val timeout: Duration = 5.seconds) {

  def await(timeout: Duration = timeout): T = {
    Await.result(future, timeout)
  }

  // convenience methods to avoid name conflicts
  def get(timeout: Duration = timeout): T = await(timeout=timeout)
  def block(timeout: Duration = timeout): T = await(timeout=timeout)
  def sync(timeout: Duration = timeout): T = await(timeout=timeout)

  /** returns a future that fails only when this job does not complete in given timeout */
  def timeout(timeout: Duration = timeout)(implicit context: ExecutionContext): Future[T] = {
    Future.firstCompletedOf(Seq(future, com.kakao.mango.concurrent.timeout(timeout)))
  }

}


