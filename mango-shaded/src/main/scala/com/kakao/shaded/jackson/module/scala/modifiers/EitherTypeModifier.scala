package com.kakao.shaded.jackson.module.scala.modifiers

import java.lang.reflect.Type

import com.kakao.shaded.jackson.databind.JavaType
import com.kakao.shaded.jackson.databind.`type`.{ReferenceType, TypeFactory, TypeBindings, TypeModifier}
import com.kakao.shaded.jackson.module.scala.JacksonModule

private object EitherTypeModifier extends TypeModifier with GenTypeModifier {
  val EITHER = classOf[Either[AnyRef, AnyRef]]

  override def modifyType(typ: JavaType, jdkType: Type, context: TypeBindings, typeFactory: TypeFactory): JavaType = {
    if (typ.isReferenceType || typ.isContainerType) typ

    if (classObjectFor(jdkType).exists(EITHER.isAssignableFrom)) {
      ReferenceType.upgradeFrom(typ, typ)
    } else typ
  }
}

trait EitherTypeModifierModule extends JacksonModule {
  this += EitherTypeModifier
}
