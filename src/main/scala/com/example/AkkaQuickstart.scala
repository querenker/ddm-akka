package com.example

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import org.rogach.scallop.{ScallopConf, ScallopOption}

import scala.collection.mutable.ListBuffer


class MasterConf(arguments: Seq[String]) extends ScallopConf(arguments) {
  val workers: ScallopOption[Int] = opt[Int](required = true, descr = "number of local workers")
  val slaves: ScallopOption[Int] = opt[Int](required = true, descr = "number of slaves to wait for")
  val input: ScallopOption[String] = opt[String](required = true, descr = "input data file")
  verify()
}

class SlaveConf(arguments: Seq[String]) extends ScallopConf(arguments) {
  val workers: ScallopOption[Int] = opt[Int](required = true, descr = "number of local workers")
  val host: ScallopOption[String] = opt[String](required = true, descr = "IP address of the master system")
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

    var passwords = new ListBuffer[String]()
    var geneSequences = new ListBuffer[String]()

    val bufferedSource = scala.io.Source.fromFile(argumentConf.input())
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
    val taskManager = system.actorOf(Actors.TaskManager.props(passwords = passwords.toVector, geneSequences = geneSequences.toVector), "taskManager")
    //supervisor ! StartSolving(20)
    //supervisor ! StartMatching(20)
  }

  def startSlave(argumentConf: SlaveConf): Unit = {
    val config = ConfigFactory.load.getConfig("SlaveSystem")
    val system = ActorSystem("SlaveSystem", config)
    val supervisor = system.actorOf(Actors.Supervisor.props(masterIp = argumentConf.host(), numWorkers = argumentConf.workers()), "worker")
  }

  class WrongCommandLineArgumentError extends RuntimeException("the first command line argument has to be `master` or `slave`")

}

