package com.kakao.mango.logging

import java.util.Properties

import com.kakao.mango.text.Resource

import scala.collection.JavaConversions._
import scala.util.Try

/** If the underlying logging library is Log4j, apply the configurations from log4j.properties to all appenders */
trait Log4jConfigurer {
  def logger: Logger

  {
    Try((
      Class.forName("org.apache.log4j.Logger"),
      Class.forName("org.apache.log4j.PropertyConfigurator"),
      Resource.properties("log4j.properties")
    )).foreach {
      case (clazz, configurator, properties) =>
        val root = clazz.getMethod("getRootLogger").invoke(null)
        val appenders = clazz.getMethod("getAllAppenders").invoke(root).asInstanceOf[java.util.Enumeration[_]]
        if (!appenders.hasMoreElements) {
          configurator.getMethod("configure", classOf[Properties]).invoke(null, properties)
          logger.info(s"configured log4j with PropertyConfigurator; log4j.properties had ${properties.size()} properties")
          for (name <- properties.stringPropertyNames()) {
            logger.debug(s"log4j.properties: $name = ${properties.getProperty(name)}")
          }
        }
    }
  }

}
