package com.kakao.shaded.jackson.module.scala

import com.kakao.shaded.jackson.module.scala.deser.TupleDeserializerModule
import com.kakao.shaded.jackson.module.scala.ser.TupleSerializerModule

/**
 * Adds support for serializing and deserializing Scala Tuples.
 */
trait TupleModule extends TupleSerializerModule with TupleDeserializerModule {
}
