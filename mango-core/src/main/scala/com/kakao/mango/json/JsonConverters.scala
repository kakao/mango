package com.kakao.mango.json

import java.io.InputStream

import com.kakao.shaded.jackson.core.{JsonParser, JsonToken}
import com.kakao.shaded.jackson.databind.{DeserializationFeature, ObjectMapper, ObjectWriter}
import com.kakao.shaded.jackson.module.afterburner.AfterburnerModule
import com.kakao.shaded.jackson.module.scala.DefaultScalaModule

import scala.language.implicitConversions
import scala.reflect._

/** A JSON conversion utility on top of Jackson Scala Module.
  * It uses reflection-based features of Jackson that may not be ideal for some Scala types,
  * but in practice, it performs reasonably fast, with Jackson being stable and reliable.
  *
  * Intended to be used as a mix-in, see [[com.kakao.mango.json]] package object as well
  */
trait JsonConverters {

  private[mango] def newMapper() = {
    val mapper = new ObjectMapper()
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    mapper.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, true)
    mapper.registerModule(new AfterburnerModule)
    mapper
  }

  /** The singleton [[ObjectMapper]], which is mostly stateless, except setDateFormat, etc.
    * It is thread-safe assuming that we don't use such methods.
    */
  private[mango] val scalaJackson = {
    val mapper = newMapper()
    mapper.registerModule(DefaultScalaModule)
    mapper
  }

  /** An [[ObjectMapper]] without using the Scala Module.
    * It will parse JSON arrays as [[java.util.ArrayList]]s, and JSON objects as
    * [[java.util.LinkedHashMap]]s, making it useful for getting mutable data structures
    */
  private[mango] val javaJackson = newMapper()

  private[mango] val factory = javaJackson.getFactory

  private lazy val pretty = scalaJackson.writerWithDefaultPrettyPrinter()

  private implicit def classTagToClass[T](tag: ClassTag[T]): Class[T] = tag.runtimeClass.asInstanceOf[Class[T]]

  /** Convert any object into a JSON-style, String-keyed map */
  def toMap(obj: Any): Map[String, Any] = scalaJackson.convertValue(obj, classOf[Map[String, Any]])

  /** Convert any object into a JSON string */
  def toJson(obj: Any): String = scalaJackson.writeValueAsString(obj)

  /** convert any object into a JSON string that is pretty-formatted */
  def toPrettyJson(obj: Any): String = pretty.writeValueAsString(obj)

  /** Convert any object into a JSON byte array */
  def serialize(obj: Any): Array[Byte] = scalaJackson.writeValueAsBytes(obj)

  /** Convert any object into a JSON byte array that is pretty-formatted */
  def serializePretty(obj: Any): Array[Byte] = pretty.writeValueAsBytes(obj)

  /** Parse a JSON byte array into given Class */
  def fromJson[T: ClassTag](src: Array[Byte]): T = scalaJackson.readValue(src, classTag[T])

  /** Parse a JSON string into given Class */
  def fromJson[T: ClassTag](src: String): T = scalaJackson.readValue(src, classTag[T])

  /** Parse a JSON byte array into given Class, using Java deserializer */
  def fromJsonJava[T: ClassTag](src: Array[Byte]): T = javaJackson.readValue(src, classTag[T])

  /** Parse a JSON string into given Class, using Java deserializer */
  def fromJsonJava[T: ClassTag](src: String): T = javaJackson.readValue(src, classTag[T])

  /** Parse a JSON byte array into a Scala Map */
  def parseJson(src: Array[Byte]): Map[String, Any] = scalaJackson.readValue(src, classOf[Map[String,Any]])

  /** Parse a JSON string into a Scala Map */
  def parseJson(src: String): Map[String, Any] = scalaJackson.readValue(src, classOf[Map[String,Any]])

  /** Parse a JSON byte array into a Java Map */
  def parseJsonJava(src: Array[Byte]): java.util.Map[String, Any] = javaJackson.readValue(src, classOf[java.util.Map[String, Any]])

  /** Parse a JSON string into a Java Map */
  def parseJsonJava(src: String): java.util.Map[String, Any] = javaJackson.readValue(src, classOf[java.util.Map[String, Any]])

  type JsonIterator = Iterator[(JsonToken, JsonTokenAccessor)]

  def streamJson(src: Array[Byte]): JsonIterator = streamJson(factory.createParser(src))
  def streamJson(src: Array[Byte], offset: Int, length: Int): JsonIterator = streamJson(factory.createParser(src, offset, length))
  def streamJson(src: InputStream): JsonIterator = streamJson(factory.createParser(src))
  def streamJson(src: String): JsonIterator = streamJson(factory.createParser(src))

  /** A wrapper to Jackson's streaming API */
  def streamJson(parser: JsonParser): JsonIterator = {
    val accessor = JsonTokenAccessor(parser)

    new Iterator[(JsonToken, JsonTokenAccessor)] {
      override def hasNext: Boolean = !parser.isClosed
      override def next(): (JsonToken, JsonTokenAccessor) = {
        val token = parser.nextToken()
        if (token == null) parser.close()
        (token, accessor)
      }
    }
  }

}
