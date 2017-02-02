package com.kakao.mango.text

import com.kakao.mango.MangoFunSuite

class ResourceSuite extends MangoFunSuite {
  test("Should read resource correctly") {
    val resource = "resource.txt"
    val lines = Resource.lines(resource).toSeq

    lines should contain ("source")
    lines should contain ("#comment")
    lines should contain ("")

    val sourceLines = Resource.sourceLines(resource).toSeq

    sourceLines shouldNot contain ("#comment")
    sourceLines shouldNot contain ("")
  }
}
