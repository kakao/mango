package com.kakao.mango.text

import java.io.{File, FileOutputStream, InputStream}
import java.util.Properties

import com.kakao.shaded.guava.io.{ByteStreams, Files}

import scala.io.{BufferedSource, Codec, Source}

/** Utilities for reading classpath resources */
object Resource {

  val loader = getClass.getClassLoader
  implicit val codec = Codec.UTF8

  def stream(resource: String)(implicit loader: ClassLoader = loader): InputStream = {
    loader.getResourceAsStream(resource)
  }

  def source(resource: String)(implicit loader: ClassLoader = loader): BufferedSource = {
    Source.fromInputStream(stream(resource))
  }

  def lines(resource: String)(implicit loader: ClassLoader = loader): Iterator[String] = {
    source(resource).getLines()
  }

  def properties(resource: String)(implicit loader: ClassLoader = loader): Properties = {
    val properties = new Properties()
    properties.load(stream(resource))
    properties
  }

  /** get lines that are not empty and does not start with a '#' */
  def sourceLines(resource: String)(implicit loader: ClassLoader = loader): Iterator[String] = {
    lines(resource).map(_.trim).filter(line => line.nonEmpty && !line.startsWith("#"))
  }

  def bytes(resource: String)(implicit loader: ClassLoader = loader): Array[Byte] = {
    ByteStreams.toByteArray(stream(resource))
  }

  def asTemporaryFile(resource: String)(implicit loader: ClassLoader = loader): File = {
    val filename = resource.substring(resource.lastIndexOf('/') + 1)
    asTemporaryFile(stream(resource), filename)
  }

  def asTemporaryFile(input: InputStream): File = asTemporaryFile(input, "resource")

  def asTemporaryFile(input: InputStream, filename: String): File = {
    val path = s"${Files.createTempDir().getAbsolutePath}/$filename"
    val output = new FileOutputStream(path)
    ByteStreams.copy(input, output)
    output.close()
    new File(path)
  }

}
