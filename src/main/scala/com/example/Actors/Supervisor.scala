package com.example.Actors

import akka.actor.{Actor, ActorSelection, Props}
import com.example.Actors.Supervisor._
import com.example.Actors.TaskManager.RegisterSupervisor

import scala.math.min

class Supervisor(masterIp: String, numWorkers: Int) extends Actor {

  val masterActor: ActorSelection = context.actorSelection(s"akka.tcp://MasterSystem@$masterIp:5150/user/taskManager")

  override def preStart(): Unit = {
    // println("Hello from remote")
    masterActor ! RegisterSupervisor(numWorkers)
  }

  override def receive: Receive = {
    case InitSupervisor(passwords, geneSequences) =>
      for (_ <- 1 to numWorkers) {
        context.actorOf(Worker.props(passwords, geneSequences, masterActor))
      }
    case Terminate() =>
      context.system.terminate()
  }
}

object Supervisor {

  def props(masterIp: String, numWorkers: Int): Props = Props(new Supervisor(masterIp, numWorkers))


  // ToDo: no leading zeros
  final val passwordRange = 1000000

  def split_range(rangeEnd: Int, numSplits: Int): Seq[(Int, Int)] = {
    val baseChunkSize = rangeEnd / numSplits
    val chunkSizeReminder = rangeEnd % numSplits
    for (i <- 1 to numSplits) yield {
      val rangeStart = (i - 1) * baseChunkSize + min(i - 1, chunkSizeReminder)
      val rangeEnd = i * baseChunkSize + min(i, chunkSizeReminder) - 1
      (rangeStart, rangeEnd)
    }
  }

  final case class InitSupervisor(passwords: Vector[String], geneSequences: Vector[String])

  final case class Terminate()
}
