package com.example

import scala.math.min

package object Actors {

  def split_range(rangeEnd: Int, numSplits: Int): Seq[(Int, Int)] = {
    val baseChunkSize = rangeEnd / numSplits
    val chunkSizeReminder = rangeEnd % numSplits
    for (i <- 1 to numSplits) yield {
      val rangeStart = (i - 1) * baseChunkSize + min(i - 1, chunkSizeReminder)
      val rangeEnd = i * baseChunkSize + min(i, chunkSizeReminder) - 1
      (rangeStart, rangeEnd)
    }
  }

}
