package com.example

import akka.actor.{ActorRef, ActorSystem}

import scala.collection.mutable.ListBuffer

object AkkaQuickstart extends App {

  import Actors.PasswordSolverSupervisor._

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

  val passwordSolverSupervisor: ActorRef = system.actorOf(Actors.PasswordSolverSupervisor.props(passwords = passwords.toList), "passwordSolverSupervisor")
  passwordSolverSupervisor ! StartSolving(20)
}
