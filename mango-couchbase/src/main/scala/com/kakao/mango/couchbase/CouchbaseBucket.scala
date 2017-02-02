package com.kakao.mango.couchbase

import com.kakao.mango.concurrent.Throttled
import com.couchbase.client.java.AsyncBucket
import com.couchbase.client.java.document.json.JsonObject
import com.couchbase.client.java.document.RawJsonDocument
import rx.Observable

import scala.collection.JavaConversions._
import scala.concurrent.Future

import com.kakao.mango.json._

import scala.reflect.ClassTag

/** A class representing a Couchbase bucket */
case class CouchbaseBucket(bucket: AsyncBucket) {

  /** Return the name of this bucket */
  def name: String = bucket.name()

  /** Insert a documents with given id and content.
    * Unlike [[put]], it may throw DocumentAlreadyExistsException
    */
  def create(id: String, content: String, expiry: Int = 0 ): Future[Any] = {
    bucket.insert(RawJsonDocument.create(id, expiry, content))
  }

  /** Update or insert a documents with given id and content */
  def put(id: String, content: String, expiry: Int = 0 ): Future[Any] = {
    bucket.upsert(RawJsonDocument.create(id, expiry, content))
  }

  /** Replace a documents with given id and content, using given CAS value.
    * if the CAS value does not match, it will throw a CASMismatchException.
    */
  def replace(id: String, content: String, cas: Long, expiry: Int = 0 ): Future[Any] = {
    bucket.replace( RawJsonDocument.create(id, expiry, content, cas ))
  }

  /** Update or insert all documents in given sequence */
  def putAll(documents: Seq[(String, String)]): Future[Any] = putAll(documents, 0)

  /** Update or insert all documents in given sequence */
  def putAll(documents: Seq[(String, String)], expiry: Int): Future[Any] = {
    val observables = for ((id, content) <- documents) yield {
      bucket.upsert(RawJsonDocument.create(id, expiry, content))
    }
    Observable.merge(observables).toList
  }

  /** Update or insert all documents in given sequence, with a throttled speed (documents per second) */
  def putAll(documents: Seq[(String, String)], rate: Double, expiry: Int = 0): Future[Any] = {
    val observables = for ((id, content) <- Throttled(documents, rate)) yield {
      bucket.upsert(RawJsonDocument.create(id, expiry, content))
    }
    Observable.merge(observables.toSeq).toList
  }

  /** Update or insert an object, converting it to JSON */
  def putObject(id: String, obj: Any, expiry: Int = 0): Future[Any] = put(id, toJson(obj), expiry)

  /** Update or insert a sequence of objects, converting them to JSON */
  def putObjects(objects: Seq[(String, Any)]): Future[Any] = putObjects(objects, 0)

  /** Update or insert a sequence of objects, converting them to JSON */
  def putObjects(objects: Seq[(String, Any)], expiry: Int): Future[Any] = putAll(objects.map {
    case (key, value) => (key, toJson(value))
  }, expiry)

  /** Update or insert a sequence of objects, converting them to JSON, with a throttled speed (documents per second) */
  def putObjects(objects: Seq[(String, Any)], rate: Double, expiry: Int = 0): Future[Any] = putAll(objects.map {
    case (key, value) => (key, toJson(value))
  }, rate, expiry)

  /** retrieve an object with given ID as a JsonObject */
  def getObject(id: String): Future[Option[JsonObject]] = {
    bucket.get(id).map[JsonObject](fn(_.content()))
  }

  /** retrieve an object with given ID as a JSON string */
  def get(id: String): Future[Option[String]] = {
    bucket.get(id).map[String](fn(_.content().toString))
  }

  /** retrieve an object with given ID, parsing it as given Class */
  def getJson[T: ClassTag](id: String): Future[Option[T]] = {
    bucket.get(id).map[T](fn(value => fromJson[T](value.content().toString)))
  }

  /** retrieve an object with given ID, parsing it as given Class, with the CAS value */
  def getJsonWithCas[T: ClassTag](id: String): Future[Option[(T,Long)]] = {
    bucket.get(id).map[(T,Long)]( fn( x => ( fromJson[T](x.content().toString), x.cas() )))
  }

  /** remove a document with given id */
  def remove(id: String): Future[Option[String]] = {
    bucket.remove(id).map[String](fn(_.content().toString))
  }

}
