package com.kakao.mango.zk

import java.util.concurrent.TimeUnit.SECONDS

import com.kakao.mango.MangoFunSuite
import org.apache.zookeeper.KeeperException

import scala.concurrent.duration.Duration
import scala.concurrent.{Promise, TimeoutException}
import scala.util.{Failure, Success}

class ZooKeeperSuite extends MangoFunSuite with TestZooKeeper {
  // the tests should be launched sequentially to have determinstic results,
  // which is done by default in scalatest.

  override val zkServerPort = 12181

  test("set and get the same value") {
    val node = zk("test", "key")

    node.deleteAllOrIgnore().block()
    node.createOrSet("value").block()
    node.string().sync() shouldBe "value"
  }

  test("get list of children correctly") {
    val node = zk("test", "key")
    node.deleteAllOrIgnore().sync()

    node.child("one").setOrCreate("oneone").block()
    node.child("two").setOrCreate("twotwo").block()
    node.child("three").setOrCreate("threethree").block()

    val children = node.children().sync().toList

    children should contain ("one")
    children should contain ("two")
    children should contain ("three")
  }

  test("getting a nonexistent node should throw an exception") {
    val node = zk("test", "key")

    node.deleteAllOrIgnore().sync()
    assertThrows[KeeperException.NoNodeException] {
      node.string().sync()
    }
  }

  test("changes on a node's data should be notified to its watcher but not after unsubscribing") {
    val node = zk("test", "key", "watching")

    node.createOrSet("first").sync()

    val promise = Promise[Unit]()

    node.watch {
      // note that this (partial) function is a callback and can be called multiple times
      case Success("second") => promise.trySuccess(())
      case Success(_) => // ignore
      case Failure(cause) => promise.failure(cause)
    }

    node.set("second").sync()

    promise.future.sync().shouldBe(())

    val observable = node.watch()
    val subscription = observable.subscribe().unsubscribe()

    node.set("third").sync()

    assertThrows[TimeoutException] {
      observable.toList.toBlocking.toFuture.sync(Duration(1, SECONDS))
    }
  }

  test("changes on a node's children should be notified to its watcher but not after unsubscribing") {
    val node = zk("test", "parent")

    node.deleteAllOrIgnore().sync()

    node.createOrSet("parentparent").sync()

    val promise = Promise[Unit]()

    node.watchChildren {
      case Success(children) if children.forall(c => c == "one" || c == "two" || c == "three") => promise.trySuccess(())
      case Failure(cause) => promise.failure(cause)
    }

    node.child("one").createOrSet("oneone").sync()
    node.child("two").createOrSet("twotwo").sync()
    node.child("three").createOrSet("threethree").sync()

    promise.future.sync().shouldBe(())

    val observable = node.watch()
    val subscription = observable.subscribe().unsubscribe()

    node.set("third").sync()

    assertThrows[TimeoutException] {
      observable.toList.toBlocking.toFuture.sync(Duration(1, SECONDS))
    }
  }
}
