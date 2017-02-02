package com.kakao.shaded.jackson.module.scala.ser

import com.kakao.shaded.jackson.databind.`type`.{TypeFactory, MapType, MapLikeType}
import com.kakao.shaded.jackson.databind.jsontype.TypeSerializer
import com.kakao.shaded.jackson.databind.ser.Serializers
import com.kakao.shaded.jackson.databind.ser.std.StdDelegatingSerializer
import com.kakao.shaded.jackson.databind.util.StdConverter
import com.kakao.shaded.jackson.databind._
import com.kakao.shaded.jackson.module.scala.modifiers.MapTypeModifierModule
import scala.collection.JavaConverters._
import scala.collection.Map

private class MapConverter(inputType: JavaType, config: SerializationConfig)
  extends StdConverter[Map[_,_],java.util.Map[_,_]]
{
  def convert(value: Map[_,_]): java.util.Map[_,_] = {
    val m = if (config.isEnabled(SerializationFeature.WRITE_NULL_MAP_VALUES)) {
      value
    } else {
      value.filter(_._2 != None)
    }
    m.asJava
  }


  override def getInputType(factory: TypeFactory) = inputType

  override def getOutputType(factory: TypeFactory) =
    factory.constructMapType(classOf[java.util.Map[_,_]], inputType.getKeyType, inputType.getContentType)
      .withTypeHandler(inputType.getTypeHandler)
      .withValueHandler(inputType.getValueHandler)
}

private object MapSerializerResolver extends Serializers.Base {

  val BASE = classOf[collection.Map[_,_]]

  override def findMapLikeSerializer(config: SerializationConfig,
                                     mapLikeType : MapLikeType,
                                     beanDesc: BeanDescription,
                                     keySerializer: JsonSerializer[AnyRef],
                                     elementTypeSerializer: TypeSerializer,
                                     elementValueSerializer: JsonSerializer[AnyRef]): JsonSerializer[_] = {


    val rawClass = mapLikeType.getRawClass

    if (!BASE.isAssignableFrom(rawClass)) null
    else new StdDelegatingSerializer(new MapConverter(mapLikeType, config))
  }

}

trait MapSerializerModule extends MapTypeModifierModule {
  this += MapSerializerResolver
}