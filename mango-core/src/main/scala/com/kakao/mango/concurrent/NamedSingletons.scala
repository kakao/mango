package com.kakao.mango.concurrent

/** a special case of KeyedSingletons where the key is type of String */
trait NamedSingletons[T] extends KeyedSingletons[String, T] {
  def newInstance(key: String): T
}

object NamedSingletons {
  def apply[T](factory: => T) = new NamedSingletons[T] {
    override def newInstance(key: String): T = factory
  }

  def apply[T](factory: String => T) = new NamedSingletons[T] {
    override def newInstance(key: String): T = factory(key)
  }
}

/** a utility to create JVM locks per given name */
class NamedLocks extends NamedSingletons[AnyRef] {
  override def newInstance(key: String): AnyRef = new AnyRef
}
