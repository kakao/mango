package com.kakao.mango.http

import com.kakao.mango.json._
import com.kakao.shaded.ning.http.client.AsyncHttpClient

import scala.concurrent.Future

/** the common interfaces for an HTTP request */
sealed abstract class Request(prepare: String => AsyncHttpClient#BoundRequestBuilder) {

  /** sends an HTTP request to given URL, with an empty body */
  def apply(url: String): Future[Response] = apply(url, -1, EMPTY_BYTE_ARRAY)

  /** sends an HTTP request to given URL, with given string as the body, along with optional headers */
  def apply(url: String, data: String, headers: (String, String)*): Future[Response] = apply(url, -1, data.bytes, headers: _*)

  /** sends an HTTP request to given URL, with a timeout */
  def apply(url: String, requestTimeoutInMs: Int): Future[Response] = apply(url, requestTimeoutInMs, EMPTY_BYTE_ARRAY)

  /** sends an HTTP request to given URL, with a timeout and given string as the body, along with optional headers */
  def apply(url: String, requestTimeoutInMs: Int, data: String, headers: (String, String)*): Future[Response] = apply(url, requestTimeoutInMs, data.bytes, headers: _*)

  /** sends an HTTP request to given URL, with a timeout and given bytes as the body, along with optional headers */
  def apply(url: String, requestTimeoutInMs: Int, data: Array[Byte], headers: (String, String)*): Future[Response] = {
    var builder = prepare(url).setBody(data)
    for ( (name, value) <- headers ) {
      builder = builder.addHeader(name, value)
    }
    if (requestTimeoutInMs >= 0) {
      builder.setRequestTimeout(requestTimeoutInMs)
    }
    builder.execute()
  }

  /** sends an HTTP request to given URL, with given object serialized as JSON, along with optional headers */
  def json(url: String, obj: Any, headers: (String, String)*): Future[Response] = {
    apply(url, toJson(obj), ("Content-Type" -> "application/json; charset=utf-8") +: headers: _*)
  }

  /** sends an HTTP request to given URL, with a timeout and given object serialized as JSON, along with optional headers */
  def json(url: String, requestTimeoutInMs: Int, obj: Any, headers: (String, String)*): Future[Response] = {
    apply(url, requestTimeoutInMs, toJson(obj), "Content-Type" -> "application/json; charset=utf-8")
  }

}

/** an object apply() methods that makes an HTTP GET request */
object Get extends Request(client.prepareGet)

/** an object apply() methods that makes an HTTP POST request */
object Post extends Request(client.preparePost)

/** an object apply() methods that makes an HTTP PUT request */
object Put extends Request(client.preparePut)

/** an object apply() methods that makes an HTTP DELETE request */
object Delete extends Request(client.prepareDelete)

/** an object apply() methods that makes an HTTP PATCH request */
object Patch extends Request(client.preparePatch)

/** an object apply() methods that makes an HTTP HEAD request */
object Head extends Request(client.prepareHead)

/** an object apply() methods that makes an HTTP OPTIONS request */
object Options extends Request(client.prepareOptions)

/** an object apply() methods that makes an HTTP CONNECT request */
object Connect extends Request(client.prepareConnect)
