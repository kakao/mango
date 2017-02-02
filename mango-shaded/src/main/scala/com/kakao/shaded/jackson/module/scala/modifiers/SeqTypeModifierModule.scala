package com.kakao.shaded.jackson.module.scala.modifiers

import com.kakao.shaded.jackson.module.scala.JacksonModule

private object SeqTypeModifier extends CollectionLikeTypeModifier {
  val BASE = classOf[Seq[Any]]
}

trait SeqTypeModifierModule extends JacksonModule {
  this += SeqTypeModifier
}