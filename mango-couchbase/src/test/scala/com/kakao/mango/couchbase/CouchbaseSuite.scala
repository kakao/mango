package com.kakao.mango.couchbase

import java.util.UUID.randomUUID

import com.couchbase.client.java.env.DefaultCouchbaseEnvironment
import com.couchbase.client.java.error.CASMismatchException
import com.kakao.mango.MangoFunSuite
import com.kakao.mango.json.toJson
import com.kakao.mango.logging.LogLevelOverrider
import org.couchbase.mock.{BucketConfiguration, CouchbaseMock}
import scala.concurrent.duration._

case class SampleDoc ( id : String )

class CouchbaseSuite extends MangoFunSuite {

  var mock: CouchbaseMock = _
  var couchbase: Couchbase = _
  var bucket: CouchbaseBucket = _

  // start a CouchbaseMock server for testing
  override protected def beforeAll(): Unit = {
    super.beforeAll()

    LogLevelOverrider.error("com.couchbase.client")

    val conf = new BucketConfiguration()
    conf.name = "default"
    mock = new CouchbaseMock("localhost", 8091, 1, 11210, 1, null, 1)
    mock.start()
    mock.waitForStartup()
    val port = mock.getBuckets.get("default").activeServers().get(0).getPort

    logger.info(s"Mock Couchbase bucket is running at port $port")

    val env = DefaultCouchbaseEnvironment.builder()
      .bootstrapCarrierDirectPort(port)
      .connectTimeout(30.seconds.toMillis)
      .build()
    couchbase = Couchbase(env, "localhost")
    bucket = couchbase.bucket("default")

    logger.info("Connected to couchbase bucket")
  }

  // stop the CouchbaseMock server
  override protected def afterAll(): Unit = {
    try super.afterAll()
    finally {
      couchbase.disconnect()
      mock.stop()
    }
  }

  test("Return JSON and CAS values correctly") {
    val key = s"mango-test-${randomUUID().toString}"
    bucket.put( key, toJson( Map("id" -> "sample" ) ), expiry=60 ).block()
    val r1 = bucket.getJson[SampleDoc](key).block()

    r1.isDefined shouldBe true
    r1.get.id shouldBe "sample"


    //val (r2, cas) = bucket.getJsonWithCas[SampleDoc](key).block()
    val tup = bucket.getJsonWithCas[SampleDoc](key).block()

    tup.isDefined shouldBe true

    val (r2, cas) = tup.get
    r2.id shouldBe "sample"


    val r = bucket.getJsonWithCas[SampleDoc]("notexisting....").block()
    true shouldBe true
  }


  test("Works correctly with CAS") {
    val key = s"mango-test-${randomUUID().toString}"

    bucket.put(key, toJson(Map("id" -> "sample")), expiry=60 ).block()

    val r = bucket.getJsonWithCas[SampleDoc](key).block()
    val (document, cas) = r.get

    document.id shouldBe "sample"

    val badCas = 1

    // CAS mismatch
    assertThrows[CASMismatchException] {
      bucket.replace(key, toJson(Map("id" -> "error")), badCas, expiry = 60).block()
    }

    // CAS match
    bucket.replace(key, toJson(Map("id" -> "success")), cas, expiry=60 ).block()
    val updated = bucket.getJsonWithCas[SampleDoc](key).block().get._1
    updated.id shouldBe "success"
  }

  test("Returns None when a non-existing key is given") {
    val key = s"mango-test-${randomUUID().toString}"
    bucket.getJsonWithCas[SampleDoc]("non-existing....").block() shouldBe None
  }

}