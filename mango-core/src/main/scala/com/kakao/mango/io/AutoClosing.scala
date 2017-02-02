package com.kakao.mango.io

import scala.language.reflectiveCalls

/**
 * enables Java's try-with-resources semantic, in a similar way.
 *
 * {{{
 * AutoClosing (
 *   new URL("http://kin.naver.com/robots.txt").openStream()
 * ) { input =>
 *   ByteStreams.copy(input, System.out)
 * }
 * }}}
 *
 * multiple resources can be opened together, using the partial function syntax.
 *
 * {{{
 * AutoClosing (
 *   new URL("http://kin.naver.com/robots.txt").openStream(),
 *   new URL("http://tip.daum.net/robots.txt").openStream()
 * ) { case (input1, input2) =>
 *   ByteStreams.copy(input1, System.out)
 *   ByteStreams.copy(input2, System.out)
 * }
 * }}}
 **/
object AutoClosing {

  class AutoCloser[R <: AnyRef, T](resources: => R) {
    def apply(action: R => T): T = {
      var res: R = null.asInstanceOf[R]
      try {
        res = resources
        action(res)
      } finally {
        close(res)
      }
    }
  }
  
  // the resource should be an [[AutoCloseable]]
  type R = AutoCloseable
  
  def apply[R1 <: R, T](resource: => R1)
    = new AutoCloser[R1, T](resource)

  def apply[R1 <: R, R2 <: R, T](res1: => R1, res2: => R2)
    = new AutoCloser[(R1, R2), T]((res1, res2))

  def apply[R1 <: R, R2 <: R, R3 <: R, T](res1: => R1, res2: => R2, res3: => R3)
    = new AutoCloser[(R1, R2, R3), T]((res1, res2, res3))
  
  def apply[R1 <: R, R2 <: R, R3 <: R, R4 <: R, T](res1: => R1, res2: => R2, res3: => R3, res4: => R4)
    = new AutoCloser[(R1, R2, R3, R4), T]((res1, res2, res3, res4))

  def apply[R1 <: R, R2 <: R, R3 <: R, R4 <: R, R5 <: R, T](res1: => R1, res2: => R2, res3: => R3, res4: => R4, res5: => R5)
    = new AutoCloser[(R1, R2, R3, R4, R5), T]((res1, res2, res3, res4, res5))

  /** close a resource, or a tuple of resources, ignoring null */
  def close(resource: Any): Unit = resource match {
    case res: R => Option(res).foreach(_.close())
    case res: Product => res.productIterator.foreach(close)
    case _ =>
  }

}
