package com.kakao.mango.concurrent

import java.lang.Thread.UncaughtExceptionHandler
import java.util.concurrent.ThreadFactory

import com.kakao.shaded.guava.util.concurrent.ThreadFactoryBuilder

/** A utility for creating a Thread Factory that creates threads named with given prefix */
object NamedThreadFactory {
  def apply(prefix: String, daemon: Boolean = true): ThreadFactory = {
    new ThreadFactoryBuilder()
      .setDaemon(daemon)
      .setNameFormat(s"$prefix-%d")
      .setUncaughtExceptionHandler(new UncaughtExceptionHandler {
        override def uncaughtException(t: Thread, e: Throwable): Unit = {
          System.err.print(s"Uncaught ${e.getClass.getSimpleName} in thread ${t.getName}:")
          e.printStackTrace(System.err)
        }
      })
      .build()
  }
}