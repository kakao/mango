package com.kakao.mango.io

import java.io.File
import java.nio.file._

import scala.collection.JavaConversions._
import scala.collection.mutable

/**
 * Utility to traverse the file system, recursively if needed.
 * Uses the NIO2 methods in JDK 7, and does not use Java 8' Stream API,
 * to kindly provide to users a library that is compatible with both JDK 7 and 8.
 */
object FileSystems {

  /**
   * returns a scala Stream that lazily traverses given direcotry, recursively by default.
   *
   * @param dir         the path to the directory to traverse
   * @param recursive   whether to recursively traverse the directory
   */
  def entries(dir: Path, recursive: Boolean = true): Stream[Path] = {
    val maxDepth = if (recursive) Int.MaxValue else 1
    val stack = mutable.Stack[(Path, Int)]((dir, maxDepth))

    new Iterator[Iterator[Path]] {
      override def hasNext: Boolean = stack.nonEmpty
      override def next(): Iterator[Path] = {
        val (dir, depth) = stack.pop()
        Files.newDirectoryStream(dir).iterator().flatMap {
          case entry if Files.isDirectory(entry) =>
            if (depth > 1) stack.push((entry, depth - 1))
            Nil
          case entry => Some(entry)
        }
      }
    }.toStream.flatten
  }

  def entries(dir: File): Stream[File] = entries(dir.toPath, recursive = true).map(_.toFile)
  def entries(dir: File, recursive: Boolean): Stream[File] = entries(dir.toPath, recursive).map(_.toFile)
  def entries(dir: String): Stream[Path] = entries(Paths.get(dir), recursive = true)
  def entries(dir: String, recursive: Boolean): Stream[Path] = entries(Paths.get(dir), recursive)

}
