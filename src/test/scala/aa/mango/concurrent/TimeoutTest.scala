package com.kakao.mango.concurrent

import java.nio.charset.StandardCharsets
import java.nio.file.{StandardOpenOption, Paths, Files}
import java.util.concurrent.TimeoutException

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.collection.JavaConverters._

object TimeoutTest extends App {

  val duration = 60
  val rate = 10000
  val dt = 3

  val futures = for (i <- Throttled(1 to duration*rate, rate)) yield {
    val started = System.nanoTime()
    val future = timeout(dt.seconds)

    future.recover {
      case e: TimeoutException =>
        (System.nanoTime() - started) / 1e9 - dt
    }
  }

  val delays = Future.sequence(futures).sync()

  val lines = delays.map(_.toString).toIterable.asJava

  Files.write(Paths.get(s"rate=$rate.txt"), lines, StandardCharsets.UTF_8)

}
