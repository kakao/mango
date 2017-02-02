package com.kakao.mango.logging

import org.slf4j.LoggerFactory

/** A trait to be extended by any class that needs logging.
  * The logger instance will be created only when the first logging call is invoked.
  */
trait Logging extends Log4jConfigurer {
  lazy val logger = Logger(LoggerFactory.getLogger(getClass.getName.stripSuffix("$")))
}

/** A trait to be extended by any class that needs logging.
  * The logger instance will be created along with the creation of enclosing object.
  * [[Logging]] should be preferred if there is no specific reason to use [[StrictLogging]].
  */
trait StrictLogging extends Log4jConfigurer {
  val logger = Logger(LoggerFactory.getLogger(getClass.getName.stripSuffix("$")))
}
