package com.kakao.shaded.jackson.module.scala.ser


import com.kakao.shaded.jackson.databind.{BeanDescription, JavaType, SerializationConfig, SerializerProvider, JsonSerializer, BeanProperty}
import com.kakao.shaded.jackson.databind.ser.{Serializers, ContextualSerializer}
import com.kakao.shaded.jackson.core.JsonGenerator;
import com.kakao.shaded.jackson.module.scala.JacksonModule;

private class TupleSerializer extends JsonSerializer[Product] {
  
  def serialize(value: Product, jgen: JsonGenerator, provider: SerializerProvider)
  {
    jgen.writeStartArray()
    value.productIterator.foreach(jgen.writeObject _)
    jgen.writeEndArray()
  }
}

private object TupleSerializerResolver extends Serializers.Base {

  private val PRODUCT = classOf[Product]

  override def findSerializer(config: SerializationConfig, javaType: JavaType, beanDesc: BeanDescription) = {
    val cls = javaType.getRawClass
    if (!PRODUCT.isAssignableFrom(cls)) null else
    // If it's not *actually* a tuple, it's either a case class or a custom Product
    // which either way we shouldn't handle here.
    if (!cls.getName.startsWith("scala.Tuple")) null else
    new TupleSerializer
  }

}

trait TupleSerializerModule extends JacksonModule {
  this += (_ addSerializers TupleSerializerResolver)
}