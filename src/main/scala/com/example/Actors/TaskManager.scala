package com.example.Actors

import akka.actor.{Actor, ActorRef, Props}
import com.example.Actors.Supervisor.InitSupervisor
import com.example.Actors.TaskManager.{RegisterWorker, _}
import com.example.Actors.Worker.{MatchGeneSequence, SolvePassword}

import scala.collection.mutable
import scala.math.min

class TaskManager(passwords: Vector[String], geneSequences: Vector[String], numSupervisor: Int) extends Actor {

  // ToDo: avoid mutable state in actors
  private val workers = new mutable.ListBuffer[ActorRef]()
  // ToDo: replace any
  private val tasks = new mutable.Queue[Task]()

  private var numMissingSupervisor = numSupervisor
  private var numWorkersToWait = 0

  override def preStart(): Unit = {
    preparePasswordTasks()
    prepareGeneMatchingTasks()
  }

  def preparePasswordTasks(): Unit = {
    for ((rangeStart, rangeEnd) <- split_range(passwordRange, numPasswordTasks)) {
      tasks += SolvePassword((rangeStart, rangeEnd))
    }
  }

  def prepareGeneMatchingTasks(): Unit = {
    for ((rangeStart, rangeEnd) <- split_range(geneSequences.length, numGeneMatchingTasks)) {
      tasks += MatchGeneSequence(rangeStart, rangeEnd)
    }
  }

  override def receive: Receive = {
    case msg: String =>
      println(s"TaskManager received message: $msg")
    case RegisterSupervisor(numWorkers) =>
      numMissingSupervisor -= 1
      numWorkersToWait += numWorkers
      println(s"Supervisor with $numWorkers registered")
      sender() ! InitSupervisor(passwords, geneSequences)
    case RegisterWorker() =>
      val worker = sender()
      workers += worker
      println(s"Add $worker to available workers")
      if (numMissingSupervisor < 1) {
        Some((workers.length compare numWorkersToWait).signum) collect {
          case 0 => startDelegating()
          case 1 => sendTask(worker)
        }
      }
    case PasswordResult(passwordHash, passwordEncrypted) =>
      println(s"Got result for $passwordHash: $passwordEncrypted")
      sendTask(sender())
    case MatchingResult(personId, partnerId) =>
      println(s"${personId}s best partner is $partnerId")
      sendTask(sender())

  }

  def startDelegating(): Unit = {
    for (worker <- workers) {
      worker ! tasks.dequeue()
    }
  }

  def sendTask(worker: ActorRef): Unit = {
    //ToDo: Handle case if queue is empty
    if (tasks.nonEmpty) {
      worker ! tasks.dequeue()
    }
  }
}

object TaskManager {

  final val numPasswordTasks = 20
  final val numGeneMatchingTasks = 20
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

  def props(passwords: Vector[String], geneSequences: Vector[String], numSupervisor: Int): Props = Props(new TaskManager(passwords, geneSequences, numSupervisor))

  final case class PasswordResult(passwordHash: String, passwordEncrypted: String)

  final case class RegisterWorker()

  final case class RegisterSupervisor(numWorkers: Int)

  final case class MatchingResult(personId: Int, partnerId: Int)
}
