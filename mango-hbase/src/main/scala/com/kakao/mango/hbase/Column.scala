package com.kakao.mango.hbase

import java.util.Arrays.{hashCode => arrayHash, equals => arrayEquals}

import com.kakao.shaded.hbase.async.KeyValue

/** An HBase column specified by family and qualifier in byte array type.
  * Since JVM checks the equality of array using only reference equality,
  * it implements its own hashCode and equals, to be used as keys in Maps.
  */
case class ByteColumn(family: Array[Byte], qualifier: Array[Byte]) {

  override def hashCode(): Int = arrayHash(family) ^ arrayHash(qualifier)

  override def equals(obj: scala.Any): Boolean = obj match {
    case ByteColumn(f, q) => arrayEquals(family, f) && arrayEquals(qualifier, q)
    case _ => false
  }

  def string = StringColumn(family.string, qualifier.string)

}

object ByteColumn {
  def apply(kv: KeyValue): ByteColumn = new ByteColumn(kv.family(), kv.qualifier())
}

/** An HBase column specified by family and qualifier in String type */
case class StringColumn(family: String, qualifier: String)

object StringColumn {
  def apply(kv: KeyValue): StringColumn = new StringColumn(kv.family().string, kv.qualifier().string)
}
