package com.kakao.shaded.jackson.module.scala

import com.kakao.shaded.jackson.module.scala.deser.OptionDeserializerModule
import com.kakao.shaded.jackson.module.scala.ser.OptionSerializerModule

/**
 * Adds support for serializing and deserializing Scala Options.
 */
trait OptionModule extends OptionSerializerModule with OptionDeserializerModule {
}
