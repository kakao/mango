package com.kakao.mango.util

import java.nio.charset.StandardCharsets

import scala.language.implicitConversions

trait Conversions {

  case class Bytesable(str: String) {
    val bytes = str.getBytes(StandardCharsets.UTF_8)
  }

  implicit def toBytesable(str: String): Bytesable = Bytesable(str)

  case class Stringable(array: Array[Byte]) {
    val string = new String(array, StandardCharsets.UTF_8)
  }

  implicit def toStringable(array: Array[Byte]): Stringable = Stringable(array)

}

object Conversions extends Conversions
