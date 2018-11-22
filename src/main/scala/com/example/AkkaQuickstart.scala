package com.example

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory

import scala.collection.mutable.ListBuffer

object AkkaQuickstart extends App {

  def startMaster(): Unit = {
    val numberOfWorkers = 4
    val inputFile = "students.csv"

    var passwords = new ListBuffer[String]()
    var geneSequences = new ListBuffer[String]()

    val bufferedSource = scala.io.Source.fromFile(inputFile)
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

    val config = ConfigFactory.load.getConfig("MasterSystem")
    val system = ActorSystem("MasterSystem", config)

    println("Start TaskManager")
    val taskManager = system.actorOf(Actors.TaskManager.props(), "taskManager")
    //val supervisor: ActorRef = system.actorOf(Actors.Supervisor.props(passwords = passwords.toVector, geneSequences = geneSequences.toVector), "supervisor")
    //supervisor ! StartSolving(20)
    //supervisor ! StartMatching(20)
  }

  def startSlave(): Unit = {
    val config = ConfigFactory.load.getConfig("SlaveSystem")
    val system = ActorSystem("SlaveSystem", config)
    val worker = system.actorOf(Actors.Worker.props(), "worker")
  }

  override def main(args: Array[String]): Unit = {
    if (args(0) == "Master") {
      startMaster()
    }
    else {
      startSlave()
    }
  }



}
