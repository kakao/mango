package com.kakao.shaded.jackson.module.scala.modifiers

import com.kakao.shaded.jackson.module.scala.JacksonModule

private object ScalaIteratorTypeModifier extends CollectionLikeTypeModifier {
  val BASE = classOf[Iterator[Any]]
}

trait IteratorTypeModifierModule extends JacksonModule {
  this += ScalaIteratorTypeModifier
}
