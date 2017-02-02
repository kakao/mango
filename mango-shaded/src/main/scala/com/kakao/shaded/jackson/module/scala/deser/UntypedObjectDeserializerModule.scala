package com.kakao.shaded.jackson.module.scala.deser

import java.{util => ju}

import com.kakao.shaded.jackson.core.JsonParser
import com.kakao.shaded.jackson.core.`type`.TypeReference
import com.kakao.shaded.jackson.databind.`type`.{CollectionType, MapType}
import com.kakao.shaded.jackson.databind.deser.{Deserializers, std}
import com.kakao.shaded.jackson.databind.{BeanDescription, DeserializationConfig, DeserializationContext, DeserializationFeature, JavaType, JsonDeserializer}
import com.kakao.shaded.jackson.module.scala.JacksonModule

object UntypedObjectDeserializer
{
  lazy val SEQ = new TypeReference[collection.Seq[Any]] {}
  lazy val MAP = new TypeReference[collection.Map[String,Any]] {}
}

private class UntypedObjectDeserializer extends std.UntypedObjectDeserializer(null, null) {

  private var _mapDeser: JsonDeserializer[AnyRef] = _
  private var _listDeser: JsonDeserializer[AnyRef] = _

  override def resolve(ctxt: DeserializationContext) {
    super.resolve(ctxt)
    val anyRef = ctxt.constructType(classOf[AnyRef])
    val string = ctxt.constructType(classOf[String])
    val factory = ctxt.getFactory
    val tf = ctxt.getTypeFactory
    _mapDeser = ctxt.findRootValueDeserializer(
      factory.mapAbstractType(ctxt.getConfig, tf.constructMapLikeType(classOf[collection.Map[_,_]], string, anyRef)))
    _listDeser = ctxt.findRootValueDeserializer(
      factory.mapAbstractType(ctxt.getConfig, tf.constructCollectionLikeType(classOf[collection.Seq[_]], anyRef)))
  }

  override def mapArray(jp: JsonParser, ctxt: DeserializationContext): AnyRef = {
    if (ctxt.isEnabled(DeserializationFeature.USE_JAVA_ARRAY_FOR_JSON_ARRAY)) {
      mapArrayToArray(jp, ctxt)
    }
    else {
      _listDeser.deserialize(jp, ctxt)
    }
  }

  override def mapObject(jp: JsonParser, ctxt: DeserializationContext): AnyRef = {
    _mapDeser.deserialize(jp, ctxt)
  }
}


private object UntypedObjectDeserializerResolver extends Deserializers.Base {

  lazy val OBJECT = classOf[AnyRef]
  
  override def findBeanDeserializer(javaType: JavaType,
                                    config: DeserializationConfig,
                                    beanDesc: BeanDescription) =
    if (!OBJECT.equals(javaType.getRawClass)) null
    else new UntypedObjectDeserializer
}

trait UntypedObjectDeserializerModule extends JacksonModule {
  this += (_ addDeserializers UntypedObjectDeserializerResolver)
}
