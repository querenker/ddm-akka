package com.example

import akka.actor.{ActorRef, ActorSystem}
import com.example.Actors.PasswordSolverSupervisor.StartSolving
import com.example.Actors.RnaMatchSupervisor.StartMatching

import scala.collection.mutable.ListBuffer

object AkkaQuickstart extends App {

  val numberOfWorkers = 4
  val inputFile = "students.csv"

  var passwords = new ListBuffer[String]()
  var geneSequences = new ListBuffer[String]()

  val bufferedSource = io.Source.fromFile(inputFile)
  for (line <- bufferedSource.getLines().drop(1)) {
    val cols = line.split(";").map(_.trim)
    // ToDo: Handle empty line?
    if (!cols(0).isEmpty) {
      passwords += cols(2)
      geneSequences += cols(3)
    }
  }
  // ToDo: automatic close (with?)
  bufferedSource.close

  val system: ActorSystem = ActorSystem("masterSystem")

  val passwordSolverSupervisor: ActorRef = system.actorOf(Actors.PasswordSolverSupervisor.props(passwords = passwords.toVector), "passwordSolverSupervisor")
  passwordSolverSupervisor ! StartSolving(20)
  val rnaMatchSupervisor = system.actorOf(Actors.RnaMatchSupervisor.props(geneSequences = geneSequences.toVector), "rnaMatchSupervisor")
  rnaMatchSupervisor ! StartMatching(20)
}
