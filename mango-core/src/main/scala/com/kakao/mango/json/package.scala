package com.kakao.mango

import scala.language.implicitConversions

/** enables the usage of JSON conversions by importing this package
  *
  * {{{
  *   import com.kakao.mango.json._
  *
  *   case class Example(hello: String)
  *
  *   val obj = Example("world")
  *   println(toJson(obj))   // prints {"hello":"world"}
  *
  *   val json = """{"hello":"world"}"""
  *   fromJson[Example](json)   // returns Example("world")
  *   parseJson(json)           // returns Map("hello" -> "world")
  * }}}
  */
package object json extends JsonConverters
