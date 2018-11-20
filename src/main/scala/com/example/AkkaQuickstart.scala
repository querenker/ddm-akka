package com.example

import akka.actor.{ActorRef, ActorSystem}
import com.example.Actors.Supervisor.{StartMatching, StartSolving}

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

  val supervisor: ActorRef = system.actorOf(Actors.Supervisor.props(passwords = passwords.toVector, geneSequences = geneSequences.toVector), "passwordSolverSupervisor")
  supervisor ! StartSolving(20)
  supervisor ! StartMatching(20)
}
