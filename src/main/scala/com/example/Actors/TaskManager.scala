package com.example.Actors

import akka.actor.{Actor, ActorRef, Props}
import com.example.Actors.Supervisor.InitSupervisor
import com.example.Actors.TaskManager.{RegisterWorker, _}
import com.example.Actors.Worker.{CheckLinearCombination, MatchGeneSequence, SolvePassword, StartMining}

import scala.collection.mutable
import scala.math.min
import scala.util.Random.shuffle

class TaskManager(passwords: Vector[String], geneSequences: Vector[String], numSupervisor: Int, numMasterWorkers: Int) extends Actor {

  // ToDo: avoid mutable state in actors
  private val workers = new mutable.ListBuffer[ActorRef]()
  // ToDo: replace any
  private val tasks = new mutable.Queue[Task]()

  private val numLinearCombination = 1L << passwords.length

  private var numMissingSupervisor = numSupervisor
  private var numWorkersToWait = numMasterWorkers

  private val passwordResults = Array.ofDim[Int](passwords.length)
  private var missingPasswords = passwords.length

  private val matchingResults = Array.ofDim[Int](passwords.length)

  private var linearCombinationFound = false
  private var combinationResult = 0L

  private var missingHashValues = passwords.length

  private var startTime = 0L

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
          case 0 =>
            startTime = System.currentTimeMillis()
            startDelegating()
          case 1 => sendTask(worker)
        }
      }
    case PasswordResult(userId, passwordEncrypted) =>
      println(s"Got result for $userId: $passwordEncrypted")
      passwordResults(userId - 1) = passwordEncrypted
      missingPasswords -= 1
      if (missingPasswords < 1) prepareLinearCombinationTasks()
      sendTask(sender())
    case MatchingResult(personId, partnerId) =>
      println(s"${personId}s best partner is $partnerId")
      matchingResults(personId - 1) = partnerId
      sendTask(sender())
    case LinearCombinationResult(combination: Long) =>
      println(s"Linear Combination found: $combination")
      if (!linearCombinationFound) {
        linearCombinationFound = true
        combinationResult = combination
        // ToDo: better way to empty queue?
        tasks.dequeueAll(_ => true)
        prepareHashTasks()
      } else {
        sendTask(sender())
      }
    case HashMiningResult(personId, hash) =>
      println(s"Hash for $personId found: $hash")
      missingHashValues -= 1
      if (missingHashValues == 0) {
        println(s"Final result computed in ${System.currentTimeMillis() - startTime} milliseconds")
      }
      sendTask(sender())
    case NextTask() =>
      sendTask(sender())

  }

  def prepareHashTasks(): Unit = {
    for (i <- passwords.indices) {
      tasks += StartMining(i + 1, matchingResults(i), ((combinationResult >> i) & 1).toInt)
    }
    startDelegating()
  }

  def prepareLinearCombinationTasks(): Unit = {
    val targetSum: Long = passwordResults.sum / 2
    for ((rangeStart, rangeEnd) <- shuffle(split_range_long(numLinearCombination, numLinearCombinationTasks))) {
      tasks += CheckLinearCombination((rangeStart, rangeEnd), passwordResults, targetSum)
    }
  }

  def startDelegating(): Unit = {
    for (worker <- workers) {
      sendTask(worker)
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
  final val numLinearCombinationTasks = 2000000

  final val passwordRange = 1000000

  // ToDo: Reduce code duplication: Templates?
  def split_range_long(rangeEnd: Long, numSplits: Int): Seq[(Long, Long)] = {
    val baseChunkSize = rangeEnd / numSplits
    val chunkSizeReminder = rangeEnd % numSplits
    for (i <- 1 to numSplits) yield {
      val rangeStart = (i - 1) * baseChunkSize + min(i - 1, chunkSizeReminder)
      val rangeEnd = i * baseChunkSize + min(i, chunkSizeReminder) - 1
      (rangeStart, rangeEnd)
    }
  }

  def split_range(rangeEnd: Int, numSplits: Int): Seq[(Int, Int)] = {
    val baseChunkSize = rangeEnd / numSplits
    val chunkSizeReminder = rangeEnd % numSplits
    for (i <- 1 to numSplits) yield {
      val rangeStart = (i - 1) * baseChunkSize + min(i - 1, chunkSizeReminder)
      val rangeEnd = i * baseChunkSize + min(i, chunkSizeReminder) - 1
      (rangeStart, rangeEnd)
    }
  }


  final case class PasswordResult(userId: Int, passwordEncrypted: Int)

  final case class RegisterWorker()

  final case class RegisterSupervisor(numWorkers: Int)

  final case class MatchingResult(personId: Int, partnerId: Int)

  final case class LinearCombinationResult(combination: Long)

  final case class HashMiningResult(personId: Int, hash: String)

  final case class NextTask()

  def props(passwords: Vector[String], geneSequences: Vector[String], numSupervisor: Int, numMasterWorkers: Int): Props = Props(new TaskManager(passwords, geneSequences, numSupervisor, numMasterWorkers))
}
