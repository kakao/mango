package com.kakao.mango.hashing

import com.kakao.mango.MangoFunSuite

class HashSuite extends MangoFunSuite {

  test("mmh3_32 works correctly") {
    val result = Murmur3_32("abcd")
    result.length shouldBe 8

    val result2 = Murmur3_32("abcd")
    result2.length shouldBe 8

    result shouldBe result2

    val result3 = Murmur3_32("foo")
    result3 shouldBe "20c4a5f6"
  }

  test("mmh3_128 works correctly") {
    val result = Murmur3_128("abcd")
    result.length shouldBe 32

    val result2 = Murmur3_128("abcd")
    result2.length shouldBe 32

    result shouldBe result2
    result shouldBe "4fcd5646d6b77bb875e87360883e00f2"
  }

  test("md5 works correctly") {
    val result = Md5("abcd")
    result shouldBe "e2fc714c4727ee9395f324cd2e7f331f"

    val result2 = Md5("abcde")
    result2 shouldBe "ab56b4d92b40713acc5af89985d4b786"
  }

  test("sha256 works correctly") {
    val result = Sha256("abcd")
    result shouldBe "88d4266fd4e6338d13b845fcf289579d209c897823b9217da3e161936f031589"

    val result2 = Sha256("abcde")
    result2 shouldBe "36bbe50ed96841d10443bcb670d6554f0a34b761be67ec9c4a8ad2c0c44ca42c"
  }

  test("sha512 works correctly") {
    val result = Sha512("abcd")
    result shouldBe "d8022f2060ad6efd297ab73dcc5355c9b214054b0d1776a136a669d26a7d3b14f73aa0d0ebff19ee333368f0164b6419a96da49e3e481753e7e96b716bdccb6f"

    val result2 = Sha512("abcde")
    result2 shouldBe "878ae65a92e86cac011a570d4c30a7eaec442b85ce8eca0c2952b5e3cc0628c2e79d889ad4d5c7c626986d452dd86374b6ffaa7cd8b67665bef2289a5c70b0a1"
  }

}
