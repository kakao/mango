package com.kakao.shaded.jackson.module.scala.modifiers

import java.lang.reflect.Type

import com.kakao.shaded.jackson.databind.JavaType
import com.kakao.shaded.jackson.databind.`type`.{MapLikeType, TypeBindings, TypeFactory, TypeModifier}

import com.kakao.shaded.jackson.module.scala.JacksonModule

private object MapTypeModifer extends TypeModifier with GenTypeModifier {

  val BASE = classOf[collection.Map[_,_]]

  override def modifyType(originalType: JavaType, jdkType: Type, context: TypeBindings, typeFactory: TypeFactory) = {
    if (classObjectFor(jdkType).exists(BASE.isAssignableFrom)) {
      val keyType = originalType.containedTypeOrUnknown(0)
      val valueType = originalType.containedTypeOrUnknown(1)
      MapLikeType.upgradeFrom(originalType, keyType, valueType)
    } else originalType
  }

}

/**
 * @author Christopher Currie <ccurrie@impresys.com>
 */
trait MapTypeModifierModule extends JacksonModule {
  this += MapTypeModifer
}