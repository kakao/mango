package com.kakao.mango.reflect

import java.lang.reflect.{Constructor, Method, Field}
import java.lang.reflect.Modifier._

import scala.reflect._

/** A utility for getting Field, Method, Constructor instances using reflection.
  * It can be also used with private, protected, final fields and methods.
  */
object Accessible {

  val modifiers: Field = {
    val field = classOf[Field].getDeclaredField("modifiers")
    field.setAccessible(true)
    field
  }

  def field[T: ClassTag](name: String): Field = {
    field(classTag[T].runtimeClass, name)
  }

  def field(clazz: Class[_], name: String): Field = {
    val field = clazz.getDeclaredField(name)
    field.setAccessible(true)
    modifiers.setInt(field, field.getModifiers & ~FINAL)
    field
  }

  def method[T: ClassTag](name: String, parameterTypes: Class[_]*): Method = {
    method(classTag[T].runtimeClass, name, parameterTypes: _*)
  }

  def method(clazz: Class[_], name: String, parameterTypes: Class[_]*): Method = {
    val method = clazz.getDeclaredMethod(name, parameterTypes: _*)
    method.setAccessible(true)
    method
  }

  def constructor[T](clazz: Class[T], parameterTypes: Class[_]*): Constructor[T] = {
    val constructor = clazz.getDeclaredConstructor(parameterTypes: _*)
    constructor.setAccessible(true)
    constructor
  }

  def firstConstructor[T: ClassTag]: Constructor[T] = {
    firstConstructor(classTag[T].runtimeClass.asInstanceOf[Class[T]])
  }

  def firstConstructor[T](clazz: Class[T]): Constructor[T] = {
    val constructor = clazz.getDeclaredConstructors()(0).asInstanceOf[Constructor[T]]
    constructor.setAccessible(true)
    constructor
  }

}
