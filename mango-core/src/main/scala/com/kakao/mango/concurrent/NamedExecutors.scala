package com.kakao.mango.concurrent

import java.util.concurrent.Executors._
import java.util.concurrent.ForkJoinPool.ForkJoinWorkerThreadFactory
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{ForkJoinWorkerThread, ExecutorService, ScheduledExecutorService, ForkJoinPool}
import scala.language.implicitConversions

/** A utility for creating instances of ExecutorService, with desired thread name */
object NamedExecutors {

  implicit def toRich(e: ExecutorService): RichExecutorService = new RichExecutorService(e)

  implicit def toRich(e: ScheduledExecutorService): RichScheduledExecutorService = new RichScheduledExecutorService(e)

  def scheduled(name: String, daemon: Boolean = true): RichScheduledExecutorService = {
    newSingleThreadScheduledExecutor(NamedThreadFactory(name, daemon))
  }

  def scheduledPool(name: String, size: Int, daemon: Boolean = true): RichScheduledExecutorService = {
    newScheduledThreadPool(size, NamedThreadFactory(name, daemon))
  }

  def cached(name: String, daemon: Boolean = true): RichExecutorService = {
    newCachedThreadPool(NamedThreadFactory(name, daemon))
  }

  def fixed(name: String, size: Int, daemon: Boolean = true): RichExecutorService = {
    newFixedThreadPool(size, NamedThreadFactory(name, daemon))
  }

  def single(name: String, daemon: Boolean = true): RichExecutorService = {
    newSingleThreadExecutor(NamedThreadFactory(name, daemon))
  }

  def forkJoin(name: String, size: Int, daemon: Boolean = true, asyncMode: Boolean = false): RichExecutorService = {
    val counter = new AtomicInteger()
    new ForkJoinPool(size, new ForkJoinWorkerThreadFactory {
      override def newThread(pool: ForkJoinPool): ForkJoinWorkerThread = {
        val thread = new ForkJoinWorkerThread(pool) {}
        thread.setName(s"$name-${counter.incrementAndGet()}")
        thread.setDaemon(daemon)
        thread
      }
    }, null, asyncMode)
  }

}