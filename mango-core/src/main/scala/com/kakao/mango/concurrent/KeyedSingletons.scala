package com.kakao.mango.concurrent

import java.util.concurrent.ConcurrentHashMap

/** A trait for storing singletons that are uniquely identified by given key */
trait KeyedSingletons[K, V] {

  def newInstance(key: K): V

  protected val registry = new ConcurrentHashMap[K, V]()

  def apply(key: K): V = registry.ensureEntry(key, newInstance(key))

}

/** Creates a KeyedSingletons instance from given factory method */
object KeyedSingletons {
  def apply[K, V](factory: => V) = new KeyedSingletons[K, V] {
    override def newInstance(key: K): V = factory
  }

  def apply[K, V](factory: K => V) = new KeyedSingletons[K, V] {
    override def newInstance(key: K): V = factory(key)
  }
}
