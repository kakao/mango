package com.kakao.shaded.jackson.module.scala

import com.kakao.shaded.jackson.module.scala.deser.{SortedSetDeserializerModule, UnsortedSetDeserializerModule}

/**
  * Created by cloudera on 6/24/16.
  */
trait SetModule extends UnsortedSetDeserializerModule with SortedSetDeserializerModule {

}
