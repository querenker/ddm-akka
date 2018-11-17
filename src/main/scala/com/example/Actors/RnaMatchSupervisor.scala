package com.example.Actors

import akka.actor.{Actor, ActorRef, Props}
import com.example.Actors.RnaMatchSupervisor.{MatchingResult, StartMatching}
import com.example.Actors.RnaMatchWorker.MatchGeneSequence

import scala.math.min

class RnaMatchSupervisor(geneSequences: Vector[String]) extends Actor {

  override def receive: Receive = {
    case StartMatching(numWorker) =>
      val baseChunkSize = geneSequences.length / numWorker
      val chunkSizeReminder = geneSequences.length % numWorker
      for (i <- 1 to numWorker) {
        val rangeStart = (i - 1) * baseChunkSize + min(i - 1, chunkSizeReminder)
        val rangeEnd = i * baseChunkSize + min(i, chunkSizeReminder) - 1
        val worker: ActorRef = context.actorOf(RnaMatchWorker.props(geneSequences), "rnaMatchWorker" + i)
        worker ! MatchGeneSequence((rangeStart, rangeEnd))
      }
    case MatchingResult(personId, partnerId) =>
      println(s"${personId}s best partner is $partnerId")
  }
}

object RnaMatchSupervisor {

  def props(geneSequences: Vector[String]): Props = Props(new RnaMatchSupervisor(geneSequences))

  final case class StartMatching(numWorkers: Int)

  final case class MatchingResult(personId: Int, partnerId: Int)
}
