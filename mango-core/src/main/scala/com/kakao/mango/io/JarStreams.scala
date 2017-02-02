package com.kakao.mango.io

import java.util.jar.{JarEntry, JarInputStream}

object JarStreams {

  /** returns a stream of [[JarEntry]] in the [[JarInputStream]], ignoring the META-INF directory */
  def entries(input: JarInputStream): Stream[JarEntry] = {
    ZipStreams.entries(input).filter(!_.getName.startsWith("META-INF")).asInstanceOf[Stream[JarEntry]]
  }

}
