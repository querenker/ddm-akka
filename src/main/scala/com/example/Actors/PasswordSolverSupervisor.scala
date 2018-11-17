package com.example.Actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props}

import scala.math.min

class PasswordSolverSupervisor(passwords: Vector[String]) extends Actor with ActorLogging {

  import PasswordSolverSupervisor._
  import PasswordSolverWorker._

  override def receive: Receive = {
    case StartSolving(numWorker) =>
      val baseChunkSize = passwordRange / numWorker
      val chunkSizeReminder = passwordRange % numWorker
      for (i <- 1 to numWorker) {
        val rangeStart = (i - 1) * baseChunkSize + min(i - 1, chunkSizeReminder)
        val rangeEnd = i * baseChunkSize + min(i, chunkSizeReminder) - 1
        val worker: ActorRef = context.actorOf(PasswordSolverWorker.props(passwords), "passwordSolverWorker" + i)
        worker ! SolvePassword((rangeStart, rangeEnd))
      }
    case PasswordResult(passwordHash, passwordEncrypted) =>
      println(s"Got result for $passwordHash: $passwordEncrypted")
  }
}

object PasswordSolverSupervisor {
  def props(passwords: Vector[String]): Props = Props(new PasswordSolverSupervisor(passwords))

  // ToDo: passwordEncrypred maybe as Int -> smaller to send?
  final case class PasswordResult(passwordHash: String, passwordEncrypted: String)

  final case class StartSolving(numWorker: Int)

  final val passwordRange = 1000000
}