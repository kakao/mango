package com.kakao.shaded.jackson.module.scala.experimental

import com.kakao.shaded.jackson.module.scala.JacksonModule
import com.kakao.shaded.jackson.databind.introspect.{AnnotatedMember, NopAnnotationIntrospector}
import com.kakao.shaded.jackson.annotation.JsonProperty

object DefaultRequiredAnnotationIntrospector extends NopAnnotationIntrospector {

  private val OPTION = classOf[Option[_]]

  private def isOptionType(cls: Class[_]) = OPTION.isAssignableFrom(cls)

  override def hasRequiredMarker(m: AnnotatedMember) = boolean2Boolean(
    Option(m.getAnnotation(classOf[JsonProperty])).map(_.required).getOrElse(!isOptionType(m.getRawType))
  )

}

trait RequiredPropertiesSchemaModule extends JacksonModule {
  this += { _.insertAnnotationIntrospector(DefaultRequiredAnnotationIntrospector) }
}
