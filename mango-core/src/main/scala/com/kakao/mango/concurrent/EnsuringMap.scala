package com.kakao.mango.concurrent

import java.util.concurrent.ConcurrentMap
import scala.language.implicitConversions

/**
  * This class contains extension methods for ConcurrentMap.
  */
class EnsuringMap[K, V](val map: ConcurrentMap[K, V]) extends AnyVal {

  /**
    * ensures the existence of an entry or a default value in a lock-free fashion.
    *
    * @param key      the key in the ConcurrentMap to ensure the existence
    * @param default  a closure returning the default value. It may get called more than once in a highly concurrent situation,
    *                 but only one value for given key will be stored and returned in the map.
    * @return         the value of the ensured entry
    */
  def ensureEntry(key: K, default: => V): V = {
    var result = map.get(key)
    if (result == null) {
      val value = default
      result = map.putIfAbsent(key, value)
      if (result == null) {
        result = value
      }
    }
    result
  }

}
