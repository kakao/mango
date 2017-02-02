package com.kakao.mango.io

import java.util.zip.{ZipEntry, ZipInputStream}

object ZipStreams {

  /** returns a stream of [[ZipEntry]] in the [[ZipInputStream]] */
  def entries(input: ZipInputStream): Stream[ZipEntry] = {
    val entry = input.getNextEntry
    if (entry == null) Stream.empty else entry #:: entries(input)
  }

}
