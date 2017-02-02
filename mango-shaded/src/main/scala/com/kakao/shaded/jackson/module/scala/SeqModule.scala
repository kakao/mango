package com.kakao.shaded.jackson.module.scala

import com.kakao.shaded.jackson.module.scala.deser.SeqDeserializerModule
import com.kakao.shaded.jackson.module.scala.ser.IterableSerializerModule

/**
 * Adds support for serializing and deserializing Scala sequences.
 */
trait SeqModule extends IterableSerializerModule with SeqDeserializerModule {
}
