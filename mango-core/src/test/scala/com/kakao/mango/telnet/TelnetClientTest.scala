package com.kakao.mango.telnet

object TelnetClientTest extends App {
  val client = new TelnetClient("www.example.com", 80)

  client.send("GET / HTTP/1.1")
  client.send("Host: www.example.com")
  client.send("")

  client.onMessage(println)

  Thread.sleep(1000)
  client.close()

}
