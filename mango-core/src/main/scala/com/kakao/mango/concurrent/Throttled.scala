package com.kakao.mango.concurrent

/**
  * A utility for limiting the speed of for-loops, using RateLimiter
  *
  * For example, the following code:
  *
  * {{{
  *   for (i <- Throttled(1 to 25000, 10000)) {
  *     // some job to be done with 10,000 ops / sec
  *   }
  * }}}
  *
  * will run each iteration every 100 ms, taking 2.5 seconds for the loop to finish.
  * The accuracy of maxRate parameter will depend on the system,
  * but empirically maxRate will be attained within 1% error if maxRate is under a few millions,
  * and about 10% error when maxRate is tens of millions.
  *
  * It may be useful when bulk-inserting to database while limiting the throughput.
  *
  * @param sequence  The original sequence to be iterated over
  * @param maxRate   Number of iterations per second
  */
case class Throttled[T](sequence: TraversableOnce[T], maxRate: Double) extends TraversableOnce[T] {

  override def seq: TraversableOnce[T] = this

  /** overrides [[toIterator]] to perform rate-limited iteration */
  override def toIterator: Iterator[T] = new Iterator[T] {
    private val limiter = RateLimiter(maxRate)
    private val iterator = sequence.toIterator

    override def hasNext: Boolean = iterator.hasNext
    override def next(): T = { limiter.acquire(); iterator.next() }
  }

  // overridden methods using toIterator

  override def foreach[U](f: (T) => U): Unit = toIterator.foreach(f)
  override def copyToArray[B >: T](xs: Array[B], start: Int, len: Int): Unit = toIterator.copyToArray(xs, start, len)
  override def forall(p: (T) => Boolean): Boolean = toIterator.forall(p)
  override def toTraversable: Traversable[T] = toIterator.toTraversable
  override def isEmpty: Boolean = toIterator.isEmpty
  override def find(p: (T) => Boolean): Option[T] = toIterator.find(p)
  override def exists(p: (T) => Boolean): Boolean = toIterator.exists(p)
  override def toStream: Stream[T] = toIterator.toStream

  // properties of the original TraversableOnce

  override def hasDefiniteSize: Boolean = sequence.hasDefiniteSize
  override def isTraversableAgain: Boolean = sequence.isTraversableAgain

}
