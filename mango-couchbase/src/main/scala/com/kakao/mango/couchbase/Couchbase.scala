package com.kakao.mango.couchbase

import com.kakao.mango.concurrent.{NamedExecutors, NamedThreadFactory}
import com.couchbase.client.deps.io.netty.channel.nio.NioEventLoopGroup
import com.couchbase.client.java.CouchbaseCluster
import com.couchbase.client.java.env.{CouchbaseEnvironment, DefaultCouchbaseEnvironment}
import rx.Scheduler
import rx.schedulers.Schedulers

/** The entry point of mango-couchbase client library.
  *
  * It includes a thread pool configuration to be used by default.
  * By using only this thread pool in the JVM, it will prevent continuously creating threads by [[CouchbaseCluster]],
  * even if it is created and disconnected multiple times, such as in Spark Streaming applications.
  */
object Couchbase {
  val concurrency: Int = Runtime.getRuntime.availableProcessors()
  val io = new NioEventLoopGroup(concurrency, NamedThreadFactory("mango-couchbase-io"))
  val computation: Scheduler = Schedulers.from(NamedExecutors.fixed("mango-couchbase-computation", concurrency))
  val defaultEnvironment: CouchbaseEnvironment = DefaultCouchbaseEnvironment.builder()
    .ioPool(io)
    .scheduler(computation)
    .build()

  /** create a Couchbase instance using the default environment, including the thread pool. */
  def apply(nodes: String*) = new Couchbase(defaultEnvironment, nodes: _*)

  /** create a Couchbase instance using a user-provided environment */
  def apply(environment: CouchbaseEnvironment, nodes: String*) = new Couchbase(environment, nodes: _*)
}

/** A wrapper to Couchbase Java API 2.1
  * @param nodes   the list of Couchbase API nodes
  */
class Couchbase(environment: CouchbaseEnvironment, nodes: String*) {

  val cluster = CouchbaseCluster.create(environment, nodes: _*)

  def bucket(name: String, password: String = null) = CouchbaseBucket(cluster.openBucket(name, password).async())

  def disconnect() = cluster.disconnect()

}
