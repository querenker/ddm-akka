package com.example

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import org.rogach.scallop.ScallopConf

import scala.collection.mutable.ListBuffer


class MasterConf(arguments: Seq[String]) extends ScallopConf(arguments) {
  val workers = opt[Int](required = true, descr = "number of local workers")
  val slaves = opt[Int](required = true, descr = "number of slaves to wait for")
  val input = opt[String](required = true, descr = "input data file")
  verify()
}

class SlaveConf(arguments: Seq[String]) extends ScallopConf(arguments) {
  val workers = opt[Int](required = true, descr = "number of local workers")
  val host = opt[String](required = true, descr = "IP address of the master system")
  verify()
}

object AkkaQuickstart extends App {

  override def main(args: Array[String]): Unit = {

    if (args.length < 1) {
      throw new WrongCommandLineArgumentError
    } else if (args.head == "master") {
      startMaster(new MasterConf(args.drop(1)))
    } else if (args.head == "slave") {
      startSlave(new SlaveConf(args.drop(1)))
    } else {
      throw new WrongCommandLineArgumentError
    }
  }

  def startMaster(argumentConf: MasterConf): Unit = {
    val numberOfWorkers = argumentConf.workers()
    val inputFile = argumentConf.input()

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

  def startSlave(argumentConf: SlaveConf): Unit = {
    val config = ConfigFactory.load.getConfig("SlaveSystem")
    val system = ActorSystem("SlaveSystem", config)
    val worker = system.actorOf(Actors.Worker.props(), "worker")
  }

  class WrongCommandLineArgumentError extends RuntimeException("the first command line argument has to be `master` or `slave`")

}

