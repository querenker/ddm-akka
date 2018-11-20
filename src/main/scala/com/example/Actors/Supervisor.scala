package com.example.Actors

import akka.actor.{Actor, ActorRef, Props}
import com.example.Actors.PasswordSolverWorker.SolvePassword
import com.example.Actors.RnaMatchWorker.MatchGeneSequence
import com.example.Actors.Supervisor._

import scala.math.min

class Supervisor(passwords: Vector[String], geneSequences: Vector[String]) extends Actor {

  override def receive: Receive = {
    case StartSolving(numWorker) =>
      for ((rangeStart: Int, rangeEnd: Int) <- split_range(passwordRange, numWorker)) {
        val worker: ActorRef = context.actorOf(PasswordSolverWorker.props(passwords), s"passwordSolverWorker-$rangeStart-$rangeEnd")
        worker ! SolvePassword((rangeStart, rangeEnd))
      }
    case PasswordResult(passwordHash, passwordEncrypted) =>
      println(s"Got result for $passwordHash: $passwordEncrypted")
    case StartMatching(numWorker) =>
      for ((rangeStart, rangeEnd) <- split_range(geneSequences.length, numWorker)) {
        val worker: ActorRef = context.actorOf(RnaMatchWorker.props(geneSequences), s"rnaMatchWorker-$rangeStart-$rangeEnd")
        worker ! MatchGeneSequence((rangeStart, rangeEnd))
      }
    case MatchingResult(personId, partnerId) =>
      println(s"${personId}s best partner is $partnerId")
  }
}

object Supervisor {

  def props(passwords: Vector[String], geneSequences: Vector[String]): Props = Props(new Supervisor(passwords, geneSequences))

  final case class PasswordResult(passwordHash: String, passwordEncrypted: String)

  final case class StartSolving(numWorker: Int)

  // ToDo: no leading zeros
  final val passwordRange = 1000000

  final case class StartMatching(numWorkers: Int)

  final case class MatchingResult(personId: Int, partnerId: Int)

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
