package com.kakao.mango.json

import com.kakao.shaded.jackson.databind.{DeserializationFeature, ObjectMapper}
import com.kakao.shaded.jackson.module.afterburner.AfterburnerModule
import com.kakao.shaded.jackson.module.scala.DefaultScalaModule

object AfterBurnerBenchmark {

  def newMapper() = {
    val mapper = new ObjectMapper()
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    mapper.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, true)
    mapper
  }

  lazy val plain = {
    val mapper = newMapper()
    mapper.registerModule(DefaultScalaModule)
    mapper
  }

  lazy val afterburner = {
    val mapper = newMapper()
    mapper.registerModule(DefaultScalaModule)
    mapper.registerModule(new AfterburnerModule)
    mapper
  }
  

  case class Nested(a: Int, b: Double, c: Seq[Int], d: Seq[String])
  case class Model(hello: String, answer: Int, nested: Nested)

  lazy val model = Model("world", 42, Nested(1, 3.14, 1 to 100, (1 to 100).map(_.toString)))
  lazy val json = toJson(model)

  def plainDecode(): Model = {
    plain.readValue[Model](json, classOf[Model])
  }

  def plainEncode(): String = {
    plain.writeValueAsString(model)
  }

  def afterburnerDecode(): Model = {
    afterburner.readValue[Model](json, classOf[Model])
  }

  def afterburnerEncode(): String = {
    afterburner.writeValueAsString(model)
  }

}
