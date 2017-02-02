package com.kakao.mango.hbase

import com.kakao.shaded.hbase.async.{Bytes, KeyValue}

/** An HBase cell containing a byte array */
case class ByteCell(timestamp: Long, value: Array[Byte]) {
  def string = new StringCell(timestamp, value.string)
  def long = new CounterCell(timestamp, Bytes.getLong(value))
}

object ByteCell {
  def apply(kv: KeyValue) = new ByteCell(kv.timestamp(), kv.value())
}

/** An HBase cell containing a String */
case class StringCell(timestamp: Long, value: String)

object StringCell {
  def apply(kv: KeyValue) = new StringCell(kv.timestamp(), kv.value().string)
}

/** An HBase cell containing a Long */
case class CounterCell(timestamp: Long, value: Long) {
  def string = new StringCell(timestamp, value.toString)
}

object CounterCell {
  def apply(kv: KeyValue) = new CounterCell(kv.timestamp(), Bytes.getLong(kv.value()))
  def apply(bc: ByteCell) = new CounterCell(bc.timestamp, Bytes.getLong(bc.value))
}
