package com.kakao.mango.reflect

import java.lang.reflect.{Method, Constructor}

import scala.reflect.runtime.universe._
import scala.util.control.NonFatal

/** contains utility for finding out Scala's default parameters */
object DefaultParameters {

  /** find the default parameters of the first constructor of given Class */
  def apply(clazz: Class[_]): Array[AnyRef] = apply(clazz.getConstructors()(0))

  /** find the default parameters of given constructor */
  def apply(ctor: Constructor[_]): Array[AnyRef] = {
    val clazz = ctor.getDeclaringClass
    val mirror = runtimeMirror(clazz.getClassLoader)

    val count = ctor.getParameterTypes.length
    val result = new Array[AnyRef](count)

    val prefix = "$lessinit$greater$default"

    try {
      // default parameters of a constructor is in companion objects
      // companionSymbol is deprecated in Scala 2.11, but "companion" is not present in 2.10
      val companion = mirror.classSymbol(clazz).companionSymbol
      val instance = mirror.reflectModule(companion.asModule).instance

      for (i <- 0 until count) {
        util.Try(instance.getClass.getMethod(s"$prefix$$${i+1}")).foreach { method =>
          result(i) = method.invoke(instance)
        }
      }
    } catch {
      // if there is no companion objects or default parameters, return the array with null entries
      case NonFatal(e) =>
    }

    result
  }

  /** find out the default parameters of given Method */
  def apply(obj: AnyRef, method: Method): Array[AnyRef] = {
    val clazz = method.getDeclaringClass
    val name = method.getName

    val count = method.getParameterTypes.length
    val result = new Array[AnyRef](count)

    try {
      for (i <- 0 until count) {
        util.Try(clazz.getMethod(s"$name$$default$$${i+1}")).foreach { method =>
          result(i) = method.invoke(obj)
        }
      }
    } catch {
      case NonFatal(e) => // if there is no default parameters, return the array with null entries
    }

    result
  }

}

