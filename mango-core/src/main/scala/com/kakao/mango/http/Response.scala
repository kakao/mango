package com.kakao.mango.http

import java.io.InputStream

import scala.reflect.ClassTag
import com.kakao.mango.json._

/** an interface that allows access to a HTTP response message */
trait Response {

  /** the HTTP status */
  def status: Int

  /** the whole HTTP headers */
  def headers: Map[String, String]

  /** an HTTP header corresponding to a name; may throw a NoSuchElementException if no such header exists */
  def header(name: String): String

  /** the content type */
  def contentType: String

  /** the (lazily) decoded string of the response body using the UTF-8 encoding */
  def body: String

  /** the byte array representation of the response body */
  def bytes: Array[Byte]

  /** an InputStream that reads from the response body */
  def stream: InputStream

  /** the parsing result of the JSON repsonse body into given type */
  def as[T: ClassTag]: T = fromJson(bytes)

  /** the parsing result of the JSON response body into Scala's Map */
  def json: Map[String, Any] = parseJson(bytes)

  /** the string representation of this message; use [[body]] to get the full response body as a String */
  override lazy val toString = s"an HTTP $status response (${bytes.length} bytes)"

}
