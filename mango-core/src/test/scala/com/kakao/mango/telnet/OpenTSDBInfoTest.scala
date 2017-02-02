package com.kakao.mango.telnet

import com.kakao.mango.concurrent._

object OpenTSDBInfoTest extends App {

  val tsdb = new OpenTSDB("sheep008", "sheep009", "sheep010", "sheep011", "sheep012")

  tsdb.stats()
  tsdb.version()

  Thread.sleep(1000)

  tsdb.close().sync()

}
