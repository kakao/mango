package com.kakao.mango

import com.kakao.mango.concurrent.ConcurrentConverters
import com.kakao.mango.logging.Logging
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}

abstract class MangoFunSuite extends FunSuite with Matchers with BeforeAndAfterAll with Logging with ConcurrentConverters
