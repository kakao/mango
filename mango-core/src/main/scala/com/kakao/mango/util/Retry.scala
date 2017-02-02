package com.kakao.mango.util

import scala.concurrent.duration._
import scala.reflect._

/** Utility for retrying a code block that may fail. For example,
  *
  * {{{
  *   var i = 0;
  *   Retry(5) {
  *     i += 1
  *     println(s"retry #$i")
  *     throw new RuntimeException(s"retry #$i failed")
  *   }
  * }}}
  *
  * will result:
  *
  * {{{
  *   retry #1
  *   retry #2
  *   retry #3
  *   retry #4
  *   retry #5
  *   java.lang.RuntimeException: retry #5 failed
  * }}}
  */
object Retry {

  /** A utility to retry something up to n times, even if a certain exception occurs
    * It is useful when there is a possibility of exceptions and clients are responsible to retry,
    * such as Couchbase's BackPressureException, or Asynchbase's PleaseThrottleException.
    *
    * @param n      the maximum number of times to retry
    * @param delay  the amount of time delay between each retry
    * @param fn     a closure that may fail
    * @tparam E     if this exception happens, it will retry up to n times
    * @return       the result of successful execution of fn
    */
  @annotation.tailrec
  def whenException[E <: Throwable : ClassTag, T](n: Int, delay: Duration = 0.millis)(fn: => T): T = {
    val tag = classTag[E]
    val result: Option[T] = try { Some(fn) } catch { case tag(e) if n > 1 => None }
    result match {
      case Some(x) => x
      case None =>
        if (delay.toMillis > 0) {
          Thread.sleep(delay.toMillis)
        }
        whenException(n - 1, delay)(fn)
    }
  }

  /** retry up to n times, regardless of the kinds of exceptions occurred
    * @see [[whenException]]
    */
  def apply[T](n: Int, delay: Duration = 0.millis)(fn: => T): T = whenException[Exception, T](n, delay)(fn)

}
