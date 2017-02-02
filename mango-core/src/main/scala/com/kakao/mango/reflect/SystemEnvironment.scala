package com.kakao.mango.reflect

import java.util
import java.util.Collections

/** A utility for overriding System Environment variables. It will change
  * the result of System.getenv(), and should be used with extreme caution.
  *
  * {{{
  *   SystemEnvironment.set("HOME" -> "/tmp", "LANG" -> "KO_kr.UTF-8")
  *   println(System.getenv("HOME"))
  *   println(System.getenv("LANG"))
  * }}}
  */
object SystemEnvironment {

  val env = Class.forName("java.lang.ProcessEnvironment")
  val field = Accessible.field(env, "theUnmodifiableEnvironment")
  
  def set(key: String, value: String): Unit = {    
    set(key -> value)
  }
  
  def set(entries: (String, String)*): Unit = {
    val map = new util.HashMap[String, String]()
    map.putAll(System.getenv())
    for ( (key, value) <- entries ) {
      map.put(key, value)
    }
    field.set(null, Collections.unmodifiableMap[String, String](map))
  }

}
