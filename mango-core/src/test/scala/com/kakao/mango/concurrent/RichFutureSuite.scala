package com.kakao.mango.concurrent

import com.kakao.mango.MangoFunSuite

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Future, TimeoutException}
import scala.util.Try

class RichFutureSuite extends MangoFunSuite {

  test("Blocking on Future should not time out when finished within timeout") {

    val duration = 3.seconds

    val start = System.currentTimeMillis()

    val tryBlock = Try {
      Future {
        Thread.sleep(1000)
      }.block(duration)
    }

    tryBlock.isSuccess shouldBe true
  }

  test("Should block on the Future until it finishes") {
    val duration = 100.milliseconds
    val start = System.currentTimeMillis()
    futureOfThreadSleep(duration).await(duration * 2)
    val elapsedTime = System.currentTimeMillis() - start
    (elapsedTime - 100) >= 0 shouldBe true
  }

  test("Awaiting on a Future shorter than it finishes should throw a TimeoutException") {

    val duration = 100.milliseconds

    assertThrows[TimeoutException] {
      val start = System.currentTimeMillis()
      futureOfThreadSleep(duration*2).await(duration)
    }
  }

  def futureOfThreadSleep(d: Duration): Future[Long]  = {
    Future {
      Thread.sleep(d.toMillis)
      d.toMillis
    }
  }
}
