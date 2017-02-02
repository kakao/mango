package com.kakao.mango.hashing

import java.nio.charset.StandardCharsets.UTF_8

import com.kakao.shaded.guava.hash.{HashFunction, Hashing}

/** A simple Scala interface for Guava's hash implementation.
  * returns hexstrings when called, and byte arrays can be obtained using bytes().
  *
  * {{{
  *   import com.kakao.mango.hashing._
  *
  *   println(Md5("hi")) // prints "49f68a5c8493ec2c0bf489821c21fc3b"
  *   println(Murmur3_32("hi")) // prints "1a8b6fc7"
  * }}}
  */
sealed trait Hash {
  val function: HashFunction

  def apply(str: String): String = function.hashString(str, UTF_8).toString
  def apply(bytes: Array[Byte]): String = function.hashBytes(bytes).toString

  def bytes(str: String): Array[Byte] = function.hashString(str, UTF_8).asBytes()
  def bytes(bytes: Array[Byte]): Array[Byte] = function.hashBytes(bytes).asBytes()
}

case class Murmur3_32(seed: Int) extends Hash {
  val function = Hashing.murmur3_32(seed)
}

case class Murmur3_128(seed: Int) extends Hash {
  val function = Hashing.murmur3_128(seed)
}

object Md5 extends Hash {
  val function = Hashing.md5()
}

object Sha256 extends Hash {
  val function = Hashing.sha256()
}

object Sha512 extends Hash {
  val function = Hashing.sha512()
}

object Murmur3_32 extends Murmur3_32(0)
object Murmur3_128 extends Murmur3_128(0)
