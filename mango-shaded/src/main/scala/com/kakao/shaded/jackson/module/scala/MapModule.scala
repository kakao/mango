package com.kakao.shaded.jackson.module.scala

import com.kakao.shaded.jackson.module.scala.deser.{SortedMapDeserializerModule, UnsortedMapDeserializerModule}
import com.kakao.shaded.jackson.module.scala.ser.MapSerializerModule

/**
 * @author Christopher Currie <christopher@currie.com>
 */
trait MapModule extends MapSerializerModule with SortedMapDeserializerModule with UnsortedMapDeserializerModule {

}
