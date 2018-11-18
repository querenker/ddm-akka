package com.example.Actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.example.Actors.PasswordSolverSupervisor.{PasswordResult, StartSolving, passwordRange}
import com.example.Actors.PasswordSolverWorker.SolvePassword

class PasswordSolverSupervisor(passwords: Vector[String]) extends Actor with ActorLogging {

  override def receive: Receive = {
    case StartSolving(numWorker) =>
      for ((rangeStart: Int, rangeEnd: Int) <- split_range(passwordRange, numWorker)) {
        val worker: ActorRef = context.actorOf(PasswordSolverWorker.props(passwords), s"passwordSolverWorker-$rangeStart-$rangeEnd")
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