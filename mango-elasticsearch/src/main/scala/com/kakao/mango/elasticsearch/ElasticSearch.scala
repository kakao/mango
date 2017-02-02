package com.kakao.mango.elasticsearch

import com.kakao.mango.http._
import com.kakao.mango.json._

import scala.concurrent.Future

/**
  * A client for ElasticSearch, using its HTTP API.
  * This may be less efficient than its Java API,
  * but keeps the JVM classpath free of ElasticSearch and Lucene jars.
  *
  * @param url   the address of ElasticSearch HTTP server
  */
case class ElasticSearch(url: String) {

  val host = url.stripSuffix("/")

  def extractBody(response: Response): String = {
    if (response.status >= 400) {
      throw new RuntimeException(s"Bad response from ElasticSearch : ${response.status}\n${response.body}")
    }
    response.body
  }

  def put(index: String, `type`: String, id: String, document: Any): Future[String] = {
    val body = document match {
      case map: Map[_, _] => toJson(map)
      case json: String => json
    }

    Put(s"$host/$index/${`type`}/$id", body).map(extractBody)
  }

  def post(index: String, `type`: String, document: Any): Future[String] = {
    val body = document match {
      case map: Map[_, _] => toJson(map)
      case json: String => json
    }

    Post(s"$host/$index/${`type`}", body).map(extractBody)
  }

  def bulk(index: String, `type`: String, documents: Seq[Any]): Future[String] = {
    val body = documents.flatMap {
      case (key, json: String) => Seq(toJson(Map("index" -> Map("_id" -> key))), json)
      case (key, map) => Seq(toJson(Map("index" -> Map("_id" -> key))), toJson(map))
      case map: Map[_,_] => Seq("""{"index":{}}""", toJson(map))
      case json: String => Seq("""{"index":{}}""", json)
    }.mkString("", "\n", "\n")

    Post(s"$host/$index/${`type`}/_bulk", body).map(extractBody)
  }

  def get(index: String, `type`: String, id: String): Future[String] = {
    Get(s"$host/$index/${`type`}/$id").map(extractBody)
  }

  def delete(index: String, `type`: String, id: String): Future[String] = {
    Delete(s"$host/$index/${`type`}/$id").map(extractBody)
  }

}
