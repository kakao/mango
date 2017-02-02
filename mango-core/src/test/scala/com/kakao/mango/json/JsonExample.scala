package com.kakao.mango.json

object JsonExample extends App {

  case class Test(hello: String)

  val test = fromJson[Test]("""{"hello": "world"}""")

  println(test)

}
