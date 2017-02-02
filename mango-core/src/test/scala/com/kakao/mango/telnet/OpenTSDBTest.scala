package com.kakao.mango.telnet

import com.kakao.mango.concurrent._

object OpenTSDBTest extends App {

  val tsdb = new OpenTSDB("seoul016", "seoul017", "seoul018", "seoul019", "seoul020")

  val points = 200
  for (i <- 0 to points) {
    val x = 2 * Math.PI * i / points
    val y = math.sin(x)
    tsdb.put("test.jongwook", y)
    println(s"wrote ($x, $y)")
    Thread.sleep(1000)
  }

  tsdb.close().sync()

}
