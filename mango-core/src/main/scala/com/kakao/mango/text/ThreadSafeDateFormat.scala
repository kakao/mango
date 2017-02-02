package com.kakao.mango.text

import java.text.{FieldPosition, ParsePosition, SimpleDateFormat, DateFormat}
import java.util.Date
import scala.language.implicitConversions
import scala.language.reflectiveCalls

/**
 * Since [[SimpleDateFormat]] in Java is not thread-safe and cannot be called concurrently,
 * this class wraps it within a [[ThreadLocal]], while conforming to the [[DateFormat]] interface.
 *
 * Using this class is not necessary when a [[DateFormat]] is used as a local variable.
 *
 * {{{
 * import com.kakao.mango.text.ThreadSafeDateFormat
 *
 * println(ThreadSafeDateFormat("yyyy-MM-dd").format(new java.util.Date()))
 * }}}
 */
class ThreadSafeDateFormat(formatString: String) extends DateFormat {
  val format: ThreadLocal[DateFormat] = new ThreadLocal[DateFormat] {
    override def initialValue(): DateFormat = new SimpleDateFormat(formatString)
  }

  override def format(date: Date, toAppendTo: StringBuffer, fieldPosition: FieldPosition): StringBuffer
    = format.get().format(date, toAppendTo, fieldPosition)

  override def parse(source: String, pos: ParsePosition): Date
    = format.get().parse(source, pos)

  def apply(timestamp: Long): String = format(new Date(timestamp))

  def apply(date: Date): String = format(date)

  def apply(time: {def milliseconds: Long}): String = apply(time.milliseconds)
}

/** contains a convenient method to instantiate a [[ThreadSafeDateFormat]] without `new` */
object ThreadSafeDateFormat {
  def apply(format: String) = new ThreadSafeDateFormat(format)
}
