package com.example.Actors

import akka.actor.{Actor, ActorRef, Props}
import com.example.Actors.RnaMatchSupervisor.{MatchingResult, StartMatching}
import com.example.Actors.RnaMatchWorker.MatchGeneSequence

class RnaMatchSupervisor(geneSequences: Vector[String]) extends Actor {

  override def receive: Receive = {
    case StartMatching(numWorker) =>
      for ((rangeStart, rangeEnd) <- split_range(geneSequences.length, numWorker)) {
        val worker: ActorRef = context.actorOf(RnaMatchWorker.props(geneSequences), s"rnaMatchWorker-$rangeStart-$rangeEnd")
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
