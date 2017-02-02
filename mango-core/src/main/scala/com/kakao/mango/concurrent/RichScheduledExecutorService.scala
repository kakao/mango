package com.kakao.mango.concurrent

import java.util.concurrent.{Callable, ScheduledFuture, TimeUnit, ScheduledExecutorService}

import scala.concurrent.duration.Duration
import scala.concurrent.duration._
import scala.language.postfixOps

/** An extension to ScheduledExecutorService which enables using Scala lambdas */
class RichScheduledExecutorService(underlying: ScheduledExecutorService) extends RichExecutorService(underlying) with ScheduledExecutorService {

  def scheduleIn[T](delay: Duration)(command: => T): ScheduledFuture[T] = schedule(new Callable[T] {
    override def call(): T = command
  }, delay.toMillis, TimeUnit.MILLISECONDS)

  def withFixedRate[T](rate: Duration, initialDelay: Duration = 0.second)(command: => Unit) = scheduleAtFixedRate(new Runnable {
    override def run(): Unit = command
  }, initialDelay.toMillis, rate.toMillis, TimeUnit.MILLISECONDS)

  def withFixedDelay[T](delay: Duration, initialDelay: Duration = 0.second)(command: => Unit) = scheduleWithFixedDelay(new Runnable {
    override def run(): Unit = command
  }, initialDelay.toMillis, delay.toMicros, TimeUnit.MILLISECONDS)

  // delegating to underlying
  override def schedule(command: Runnable, delay: Long, unit: TimeUnit): ScheduledFuture[_] = underlying.schedule(wrap(command), delay, unit)
  override def scheduleAtFixedRate(command: Runnable, initialDelay: Long, period: Long, unit: TimeUnit): ScheduledFuture[_] = underlying.scheduleAtFixedRate(wrap(command), initialDelay, period, unit)
  override def schedule[V](callable: Callable[V], delay: Long, unit: TimeUnit): ScheduledFuture[V] = underlying.schedule(wrap(callable), delay, unit)
  override def scheduleWithFixedDelay(command: Runnable, initialDelay: Long, delay: Long, unit: TimeUnit): ScheduledFuture[_] = underlying.scheduleWithFixedDelay(wrap(command), initialDelay, delay, unit)
}