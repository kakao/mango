package com.kakao.mango.logging

import org.slf4j.helpers.BasicMarkerFactory
import org.slf4j.{Marker, Logger => Underlying}

import scala.language.experimental.macros

object Logger {
  val markerFactory = new BasicMarkerFactory
  def apply(underlying: Underlying) = new Logger(underlying)
}

/** A Logger interface that utilize the macro implementations defined in [[Macro]].
  * The message is passed as a simple String type, rather than call-by-name =>String type,
  * since the macro will put the call inside an if-block that checks if the logger is enabled.
  *
  * @param underlying   Underlying SLF4j logger
  */
final class Logger private (val underlying: Underlying) {

  val markerFactory = Logger.markerFactory
  def marker(name: String): Marker = markerFactory.getMarker(name)

  def error(message: String): Unit = macro Macro.errorMessage
  def error(message: String, cause: Throwable): Unit = macro Macro.errorMessageCause
  def error(message: String, args: AnyRef*): Unit = macro Macro.errorMessageArgs
  def error(message: String, args: Any*): Unit = error(message, args.asInstanceOf[Seq[AnyRef]]: _*)

  def warn(message: String): Unit = macro Macro.warnMessage
  def warn(message: String, cause: Throwable): Unit = macro Macro.warnMessageCause
  def warn(message: String, args: AnyRef*): Unit = macro Macro.warnMessageArgs
  def warn(message: String, args: Any*): Unit = warn(message, args.asInstanceOf[Seq[AnyRef]]:_*)

  def info(message: String): Unit = macro Macro.infoMessage
  def info(message: String, cause: Throwable): Unit = macro Macro.infoMessageCause
  def info(message: String, args: AnyRef*): Unit = macro Macro.infoMessageArgs
  def info(message: String, args: Any*): Unit = info(message, args.asInstanceOf[Seq[AnyRef]]:_*)

  def debug(message: String): Unit = macro Macro.debugMessage
  def debug(message: String, cause: Throwable): Unit = macro Macro.debugMessageCause
  def debug(message: String, args: AnyRef*): Unit = macro Macro.debugMessageArgs
  def debug(message: String, args: Any*): Unit = debug(message, args.asInstanceOf[Seq[AnyRef]]:_*)

  def trace(message: String): Unit = macro Macro.traceMessage
  def trace(message: String, cause: Throwable): Unit = macro Macro.traceMessageCause
  def trace(message: String, args: AnyRef*): Unit = macro Macro.traceMessageArgs
  def trace(message: String, args: Any*): Unit = trace(message, args.asInstanceOf[Seq[AnyRef]]:_*)

}