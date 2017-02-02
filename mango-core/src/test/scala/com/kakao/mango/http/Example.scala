package com.kakao.mango.http

import com.kakao.mango.concurrent._

object Example extends App {

  val getFuture = Get("http://example.com/")
  val getResponse = getFuture.sync()
  println(getResponse)
  getResponse.headers.foreach { case (key, value) => println(s"$key: $value") }

  val postFuture = Post.json("http://example.com/post.php?dir=example", Map("param" -> "value"), "Referer" -> "www.daum.net")
  val postResponse = postFuture.sync()
  println(postResponse)
  postResponse.headers.foreach { case (key, value) => println(s"$key: $value") }

}