package com.kakao.mango.logging

import ch.qos.logback.classic.{Level => LogbackLevel, Logger => LogbackLogger}
import com.kakao.mango.reflect.Accessible
import org.apache.log4j.{Level => Log4jLevel, Logger => Log4jLogger}
import org.slf4j.impl.Log4jLoggerAdapter
import org.slf4j.{LoggerFactory, Logger => Slf4jLogger}


sealed trait LogLevelOverrider[T] {
  def getLevel(logger: T): String
  def setLevel(logger: T, level: String)

  final def getLogger(logger: String): T = LoggerFactory.getLogger(logger).asInstanceOf[T]
  final def getLevel(logger: String): String = getLevel(getLogger(logger))
  final def setLevel(logger: String, level: String): Unit = setLevel(getLogger(logger), level)
}

/** Override the log level temporarily, regardless of whether Log4j or Logback is being used */
object LogLevelOverrider extends Logging {

  logger.debug("LogLevelOverrider initialized")

  def all(logger: String, loggers: String*): Unit = (logger +: loggers).foreach(logger => set(logger, "ALL"))
  def trace(logger: String, loggers: String*): Unit = (logger +: loggers).foreach(logger => set(logger, "TRACE"))
  def debug(logger: String, loggers: String*): Unit = (logger +: loggers).foreach(logger => set(logger, "DEBUG"))
  def info(logger: String, loggers: String*): Unit = (logger +: loggers).foreach(logger => set(logger, "INFO"))
  def warn(logger: String, loggers: String*): Unit = (logger +: loggers).foreach(logger => set(logger, "WARN"))
  def error(logger: String, loggers: String*): Unit = (logger +: loggers).foreach(logger => set(logger, "ERROR"))
  def off(logger: String, loggers: String*): Unit = (logger +: loggers).foreach(logger => set(logger, "OFF"))

  def all(loggers: Class[_]*): Unit = loggers.foreach(logger => set(logger, "ALL"))
  def trace(loggers: Class[_]*): Unit = loggers.foreach(logger => set(logger, "TRACE"))
  def debug(loggers: Class[_]*): Unit = loggers.foreach(logger => set(logger, "DEBUG"))
  def info(loggers: Class[_]*): Unit = loggers.foreach(logger => set(logger, "INFO"))
  def warn(loggers: Class[_]*): Unit = loggers.foreach(logger => set(logger, "WARN"))
  def error(loggers: Class[_]*): Unit = loggers.foreach(logger => set(logger, "ERROR"))
  def off(loggers: Class[_]*): Unit = loggers.foreach(logger => set(logger, "OFF"))

  def set(logger: String, level: String): Unit = overrider.setLevel(logger, level)
  def set(logger: Class[_], level: String): Unit = overrider.setLevel(logger.getName, level)

  def set(mapping: (String, String), mappings: (String, String)*): Unit = {
    (mapping +: mappings).foreach {
      case (logger, level) => overrider.setLevel(logger, level)
    }
  }

  def set(mappings: (Class[_], String)*): Unit = {
    mappings.foreach {
      case (logger, level) => overrider.setLevel(logger.getName, level)
    }
  }

  /** temporary override log levels while performing a task given by f, restoring the levels afterwards */
  def withLevelOverride[T](mapping: (String, String), moreMappings: (String, String)*)(f: => T): T = {
    val mappings = mapping +: moreMappings

    val oldLevels = mappings.map {
      case (logger, _) => (logger, overrider.getLevel(logger))
    }

    mappings.foreach {
      case (logger, level) => overrider.setLevel(logger, level)
    }

    try f
    finally {
      oldLevels.foreach {
        case (logger, level) => overrider.setLevel(logger, level)
      }
    }
  }

  /** temporary override log levels while performing a task given by f, restoring the levels afterwards */
  def withLevelOverride[T](mappings: (Class[_], String)*)(f: => T): T = {
    val stringMappings = mappings.map {
      case (logger, level) => (logger.getName, level)
    }
    withLevelOverride(stringMappings.head, stringMappings.tail: _*)(f)
  }

  /** provider-specific code for log level override. */
  private val overrider: LogLevelOverrider[_] = {
    val loggerClassName = LoggerFactory.getLogger(Slf4jLogger.ROOT_LOGGER_NAME).getClass.getName
    if (loggerClassName.startsWith("ch.qos.logback.classic.Logger")) {
      new LogLevelOverrider[LogbackLogger] {
        override def setLevel(logger: LogbackLogger, level: String) = logger.setLevel(LogbackLevel.toLevel(level))
        override def getLevel(logger: LogbackLogger) = logger.getEffectiveLevel.toString
      }
    } else if (loggerClassName == "org.slf4j.impl.Log4jLoggerAdapter") {
      val field = Accessible.field[Log4jLoggerAdapter]("logger")
      def logger(adapter: Log4jLoggerAdapter) = field.get(adapter).asInstanceOf[Log4jLogger]
      new LogLevelOverrider[Log4jLoggerAdapter] {
        override def setLevel(adapter: Log4jLoggerAdapter, level: String) = logger(adapter).setLevel(Log4jLevel.toLevel(level))
        override def getLevel(adapter: Log4jLoggerAdapter) = logger(adapter).getEffectiveLevel.toString
      }
    } else {
      new LogLevelOverrider[Any] {
        override def getLevel(logger: Any): String = throw new RuntimeException("Overrider not available")
        override def setLevel(logger: Any, level: String): Unit = throw new RuntimeException("Overrider not available")
      }
    }
  }

}
