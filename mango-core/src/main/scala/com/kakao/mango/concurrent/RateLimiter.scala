package com.kakao.mango.concurrent

/**
  * A utility for running something at a desired maximum rate.
  * provides acquire() method which can be called at a certain maximum rate
  * @see [[Throttled]] for usage with for-statement
  */
case class RateLimiter(maxRate: Double) {

  /** the length of one period in milliseconds */
  val step: Double = 1000 / maxRate

  /** stores the time at which this object is created */
  val epoch: Long = System.currentTimeMillis()

  /** returns the time since this object was created */
  def now(): Double = (System.currentTimeMillis() - epoch).toDouble

  /** stores the earliest time when acquire() can return */
  @volatile var nextAvailable: Double = now() + step

  /** blocks until the next available timestamp according to [[maxRate]] */
  def acquire(): Unit = {
    while (true) {
      val timestamp = now()
      val remaining = nextAvailable - timestamp
      if (remaining >= 10) {
        Thread.sleep(1)
      } else if (remaining > 1) {
        Thread.sleep(0)
      } else if (remaining > 0) {
        // busy wait
      } else {
        nextAvailable = (if (nextAvailable + 10 < timestamp) timestamp else nextAvailable) + step
        return
      }
    }
  }

}
