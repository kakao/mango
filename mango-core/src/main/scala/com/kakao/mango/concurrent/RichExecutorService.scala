package com.kakao.mango.concurrent

import java.util
import java.util.concurrent._

import scala.collection.JavaConversions._

/** Extensions to Java's ExecutorService so that Scala's labmdas can be utilized */
class RichExecutorService(underlying: ExecutorService) extends ExecutorService {

  protected def wrap[T](task: Callable[T]) = new Callable[T] {
    override def call(): T = {
      try {
        task.call()
      } catch {
        case e: Throwable =>
          System.err.print(s"Uncaught ${e.getClass.getSimpleName} in thread ${Thread.currentThread().getName}:")
          e.printStackTrace(System.err)
          throw e
      }
    }
  }

  protected def wrap(runnable: Runnable) = new Runnable {
    override def run(): Unit = {
      try {
        runnable.run()
      } catch {
        case e: Throwable =>
          System.err.print(s"Uncaught ${e.getClass.getSimpleName} in thread ${Thread.currentThread().getName}: ")
          e.printStackTrace(System.err)
          throw e
      }
    }
  }

  def submit[T](task: => T): Future[T] = submit(new Callable[T] {
    override def call(): T = task
  })

  // delegating to the underling executorservice
  override def shutdown(): Unit = underlying.shutdown()
  override def isTerminated: Boolean = underlying.isTerminated
  override def awaitTermination(timeout: Long, unit: TimeUnit): Boolean = underlying.awaitTermination(timeout, unit)
  override def shutdownNow(): util.List[Runnable] = underlying.shutdownNow()
  override def invokeAll[T](tasks: util.Collection[_ <: Callable[T]]): util.List[Future[T]] = underlying.invokeAll(tasks.map(wrap[T]))
  override def invokeAll[T](tasks: util.Collection[_ <: Callable[T]], timeout: Long, unit: TimeUnit): util.List[Future[T]] = underlying.invokeAll(tasks.map(wrap[T]), timeout, unit)
  override def invokeAny[T](tasks: util.Collection[_ <: Callable[T]]): T = underlying.invokeAny(tasks.map(wrap[T]))
  override def invokeAny[T](tasks: util.Collection[_ <: Callable[T]], timeout: Long, unit: TimeUnit): T = underlying.invokeAny(tasks.map(wrap[T]), timeout, unit)
  override def isShutdown: Boolean = underlying.isShutdown
  override def submit[T](task: Callable[T]): Future[T] = underlying.submit(wrap(task))
  override def submit[T](task: Runnable, result: T): Future[T] = underlying.submit(wrap(task), result)
  override def submit(task: Runnable): Future[_] = underlying.submit(wrap(task))
  override def execute(command: Runnable): Unit = underlying.execute(wrap(command))
}
