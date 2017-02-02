package com.kakao.mango.logging

import scala.language.experimental.macros
import scala.reflect.macros.Context

/** Macro definitions to be used in mango-core's Logger.
  * This enables an efficient usage of Logging by putting an if statement for every logging call,
  * so that the log message does not need to be evaluated if the logging is not enabled.
  * Additionally, it will put the filename and line number as SLF4j marker without any run-time overhead,
  * unlike some Java loggers that has to create a Throwable instance to get the line number.
  */
object Macro {

  def marker(c: Context) = {
    val pos = c.enclosingPosition
    pos.source.file.name + ":" + pos.line
  }

  // ERROR

  def errorMessage(c: Context)(message: c.Expr[String]) = {
    import c.universe._
    val underlying = q"${c.prefix}.underlying"
    c.Expr[Unit](q"(if ($underlying.isErrorEnabled) $underlying.error(${c.prefix}.marker(${marker(c)}), $message)): Unit")
  }

  def errorMessageCause(c: Context)(message: c.Expr[String], cause: c.Expr[Throwable]) = {
    import c.universe._
    val underlying = q"${c.prefix}.underlying"
    c.Expr[Unit](q"(if ($underlying.isErrorEnabled) $underlying.error(${c.prefix}.marker(${marker(c)}), $message, $cause)): Unit")
  }

  def errorMessageArgs(c: Context)(message: c.Expr[String], args: c.Expr[AnyRef]*) = {
    import c.universe._
    val underlying = q"${c.prefix}.underlying"
    if (args.length == 2)
      c.Expr[Unit](q"(if ($underlying.isErrorEnabled) $underlying.error(${c.prefix}.marker(${marker(c)}), $message, ${args(0)}, ${args(1)})): Unit")
    else
      c.Expr[Unit](q"(if ($underlying.isErrorEnabled) $underlying.error(${c.prefix}.marker(${marker(c)}), $message, ..$args)): Unit")
  }

  // WARN

  def warnMessage(c: Context)(message: c.Expr[String]) = {
    import c.universe._
    val underlying = q"${c.prefix}.underlying"
    c.Expr[Unit](q"if ($underlying.isWarnEnabled) $underlying.warn(${c.prefix}.marker(${marker(c)}), $message)")
  }

  def warnMessageCause(c: Context)(message: c.Expr[String], cause: c.Expr[Throwable]) = {
    import c.universe._
    val underlying = q"${c.prefix}.underlying"
    c.Expr[Unit](q"if ($underlying.isWarnEnabled) $underlying.warn(${c.prefix}.marker(${marker(c)}), $message, $cause)")
  }

  def warnMessageArgs(c: Context)(message: c.Expr[String], args: c.Expr[AnyRef]*) = {
    import c.universe._
    val underlying = q"${c.prefix}.underlying"
    if (args.length == 2)
      c.Expr[Unit](q"if ($underlying.isWarnEnabled) $underlying.warn(${c.prefix}.marker(${marker(c)}), $message, ${args(0)}, ${args(1)})")
    else
      c.Expr[Unit](q"if ($underlying.isWarnEnabled) $underlying.warn(${c.prefix}.marker(${marker(c)}), $message, ..$args)")
  }

  // INFO

  def infoMessage(c: Context)(message: c.Expr[String]) = {
    import c.universe._
    val underlying = q"${c.prefix}.underlying"
    c.Expr[Unit](q"if ($underlying.isInfoEnabled) $underlying.info(${c.prefix}.marker(${marker(c)}), $message)")
  }

  def infoMessageCause(c: Context)(message: c.Expr[String], cause: c.Expr[Throwable]) = {
    import c.universe._
    val underlying = q"${c.prefix}.underlying"
    c.Expr[Unit](q"if ($underlying.isInfoEnabled) $underlying.info(${c.prefix}.marker(${marker(c)}), $message, $cause)")
  }

  def infoMessageArgs(c: Context)(message: c.Expr[String], args: c.Expr[AnyRef]*) = {
    import c.universe._
    val underlying = q"${c.prefix}.underlying"
    if (args.length == 2)
      c.Expr[Unit](q"if ($underlying.isInfoEnabled) $underlying.info(${c.prefix}.marker(${marker(c)}), $message, ${args(0)}, ${args(1)})")
    else
      c.Expr[Unit](q"if ($underlying.isInfoEnabled) $underlying.info(${c.prefix}.marker(${marker(c)}), $message, ..$args)")
  }

  // DEBUG

  def debugMessage(c: Context)(message: c.Expr[String]) = {
    import c.universe._
    val underlying = q"${c.prefix}.underlying"
    c.Expr[Unit](q"if ($underlying.isDebugEnabled) $underlying.debug(${c.prefix}.marker(${marker(c)}), $message)")
  }

  def debugMessageCause(c: Context)(message: c.Expr[String], cause: c.Expr[Throwable]) = {
    import c.universe._
    val underlying = q"${c.prefix}.underlying"
    c.Expr[Unit](q"if ($underlying.isDebugEnabled) $underlying.debug(${c.prefix}.marker(${marker(c)}), $message, $cause)")
  }

  def debugMessageArgs(c: Context)(message: c.Expr[String], args: c.Expr[AnyRef]*) = {
    import c.universe._
    val underlying = q"${c.prefix}.underlying"
    if (args.length == 2)
      c.Expr[Unit](q"if ($underlying.isDebugEnabled) $underlying.debug(${c.prefix}.marker(${marker(c)}), $message, ${args(0)}, ${args(1)})")
    else
      c.Expr[Unit](q"if ($underlying.isDebugEnabled) $underlying.debug(${c.prefix}.marker(${marker(c)}), $message, ..$args)")
  }

  // TRACE

  def traceMessage(c: Context)(message: c.Expr[String]) = {
    import c.universe._
    val underlying = q"${c.prefix}.underlying"
    c.Expr[Unit](q"if ($underlying.isTraceEnabled) $underlying.trace(${c.prefix}.marker(${marker(c)}), $message)")
  }

  def traceMessageCause(c: Context)(message: c.Expr[String], cause: c.Expr[Throwable]) = {
    import c.universe._
    val underlying = q"${c.prefix}.underlying"
    c.Expr[Unit](q"if ($underlying.isTraceEnabled) $underlying.trace(${c.prefix}.marker(${marker(c)}), $message, $cause)")
  }

  def traceMessageArgs(c: Context)(message: c.Expr[String], args: c.Expr[AnyRef]*) = {
    import c.universe._
    val underlying = q"${c.prefix}.underlying"
    if (args.length == 2)
      c.Expr[Unit](q"if ($underlying.isTraceEnabled) $underlying.trace(${c.prefix}.marker(${marker(c)}), $message, ${args(0)}, ${args(1)})")
    else
      c.Expr[Unit](q"if ($underlying.isTraceEnabled) $underlying.trace(${c.prefix}.marker(${marker(c)}), $message, ..$args)")
  }

}
