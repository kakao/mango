package com.kakao.mango.json

import com.kakao.mango.MangoFunSuite
import com.kakao.mango.json.JsonSuite.Example

object JsonSuite {
  case class Example(hello: String, answer: Int)
}

class JsonSuite extends MangoFunSuite {

  test("getting the same object back when serializing and parsing") {
    val obj = Example("hello", 42)
    val json = toJson(obj)
    val recovered = fromJson[Example](json)
    obj shouldBe recovered
  }

}
