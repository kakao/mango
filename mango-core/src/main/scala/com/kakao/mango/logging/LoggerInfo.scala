package com.kakao.mango.logging

import com.kakao.mango.reflect.Accessible
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.{Level, Logger => LogbackLogger}
import ch.qos.logback.core.OutputStreamAppender
import ch.qos.logback.core.spi.AppenderAttachable
import org.slf4j.helpers.SubstituteLogger
import org.slf4j.{LoggerFactory, Logger => Slf4jLogger}
import com.kakao.shaded.guava.collect.Lists

import scala.collection.JavaConversions._

/** a tree data structure that shows a logger configuration */
case class LoggerInfo(
  name: String,
  level: Option[String],
  effective: String,
  appenders: Seq[AppenderInfo],
  children: Map[String, LoggerInfo]
)

case class AppenderInfo(
  name: String,
  `class`: String,
  pattern: Option[String] = None
)

/** A utility for viewing logback configuration, for debug purposes
  */
object LoggerInfo {

  val childrenField = Accessible.field[LogbackLogger]("childrenList")
  val aaiField = Accessible.field[LogbackLogger]("aai")

  def getLoggerInfo(logger: Slf4jLogger): LoggerInfo = logger match {
    case logback: LogbackLogger =>
      val childrenList = childrenField.get(logback).asInstanceOf[java.util.List[LogbackLogger]]
      val childrenSeq = Option(childrenList).map(_.toIndexedSeq).getOrElse(Seq())
      val childrenMap = childrenSeq.map(child => (child.getName.split("\\.").last, getLoggerInfo(child))).sortBy(_._1).toMap

      val aai = aaiField.get(logback).asInstanceOf[AppenderAttachable[_]]
      val appenderSeq = Option(aai).map(_.iteratorForAppenders).map(Lists.newArrayList(_).toIndexedSeq.map({
        case appender: OutputStreamAppender[_] => appender.getEncoder match {
          case encoder: PatternLayoutEncoder => AppenderInfo(appender.getName, appender.getClass.getName, Some(encoder.getPattern))
          case _ => AppenderInfo(appender.getName, appender.getClass.getName)
        }
        case appender => AppenderInfo(appender.getName, appender.getClass.getName)
      })).getOrElse(Seq())

      LoggerInfo(logback.getName, Option(logback.getLevel).map(_.toString), logback.getEffectiveLevel.toString, appenderSeq, childrenMap)

    // http://www.slf4j.org/codes.html#substituteLogger
    case tempLogger : SubstituteLogger =>
      val level = {
        if( tempLogger.isErrorEnabled ) Level.ERROR.toString
        else if ( tempLogger.isWarnEnabled ) Level.WARN.toString
        else if ( tempLogger.isInfoEnabled ) Level.INFO.toString
        else if ( tempLogger.isDebugEnabled ) Level.DEBUG.toString
        Level.TRACE.toString
      }

      LoggerInfo( tempLogger.getName, Some(level), level, Seq(), Map( ) )

    case _ => LoggerInfo( "None", None, "", Seq(), Map() )
  }

  def getAllLoggerInfo : LoggerInfo = getLoggerInfo(LoggerFactory.getLogger(Slf4jLogger.ROOT_LOGGER_NAME))

}
