package com.kakao.shaded.jackson.module.scala.modifiers

import com.kakao.shaded.jackson.module.scala.JacksonModule

private object SetTypeModifier extends CollectionLikeTypeModifier {
  val BASE = classOf[collection.Set[Any]]
}

trait SetTypeModifierModule extends JacksonModule {
  this += SetTypeModifier
}
