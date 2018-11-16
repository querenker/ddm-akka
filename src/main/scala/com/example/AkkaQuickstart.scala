package com.example

import java.math.BigInteger

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}

import scala.collection.mutable.ListBuffer
import scala.math.min


object PasswordSolverWorker {
  def props(passwords: List[String]): Props = Props(new PasswordSolverWorker(passwords))

  final case class SolvePassword(range: (Int, Int))

  def sha256Hash(hash: String): String = {
    String.format("%064x", new BigInteger(1, java.security.MessageDigest
      .getInstance("SHA-256")
      .digest(hash.getBytes("UTF-8"))))
  }

}

class PasswordSolverWorker(passwords: List[String]) extends Actor {

  import PasswordSolverSupervisor._
  import PasswordSolverWorker._

  override def receive: Receive = {
    case SolvePassword(range: (Int, Int)) =>
      solvePassword(range)
  }

  def solvePassword(range: (Int, Int)): Unit = {
    val passwordSet = passwords.toSet
    for (i <- range._1 to range._2) {
      val DEBUG = 1
      val hashValue = sha256Hash(i.toString)
      if (passwordSet.contains(hashValue)) {
        context.parent ! PasswordResult(hashValue, i.toString)
      }
    }
  }
}

object PasswordSolverSupervisor {
  def props(passwords: List[String]): Props = Props(new PasswordSolverSupervisor(passwords))

  // ToDo: passwordEncrypred maybe as Int -> smaller to send?
  final case class PasswordResult(passwordHash: String, passwordEncrypted: String)

  final case class StartSolving(numWorker: Int)

  final val passwordRange = 1000000
}

class PasswordSolverSupervisor(passwords: List[String]) extends Actor with ActorLogging {

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

object AkkaQuickstart extends App {

  import PasswordSolverSupervisor._

  val numberOfWorkers = 4
  val inputFile = "students.csv"

  var passwords = new ListBuffer[String]()

  val bufferedSource = io.Source.fromFile(inputFile)
  for (line <- bufferedSource.getLines().drop(1)) {
    val cols = line.split(";").map(_.trim)
    // ToDo: Handle empty line?
    if (!cols(0).isEmpty) {
      passwords += cols(2)
    }
  }
  // ToDo: automatic close (with?)
  bufferedSource.close

  val system: ActorSystem = ActorSystem("masterSystem")

  val passwordSolverSupervisor: ActorRef = system.actorOf(PasswordSolverSupervisor.props(passwords = passwords.toList), "passwordSolverSupervisor")
  passwordSolverSupervisor ! StartSolving(20)
}
