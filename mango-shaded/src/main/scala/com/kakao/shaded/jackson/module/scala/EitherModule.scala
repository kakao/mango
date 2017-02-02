package com.kakao.shaded.jackson.module.scala

import com.kakao.shaded.jackson.module.scala.deser.EitherDeserializerModule
import com.kakao.shaded.jackson.module.scala.ser.EitherSerializerModule

/**
  * Created by cloudera on 6/24/16.
  */
trait EitherModule extends EitherDeserializerModule with EitherSerializerModule
