package com.example.Actors

import java.math.BigInteger

import akka.actor.{Actor, ActorSelection, Props}
import com.example.Actors.TaskManager._
import com.example.Actors.Worker._
import gstlib.GeneralizedSuffixTree

import scala.collection.immutable.ListMap
import scala.util.Random

class Worker(passwords: Vector[String], geneSequences: Vector[String], masterActor: ActorSelection) extends Actor {

  override def preStart(): Unit = {
    masterActor ! RegisterWorker()
  }

  private val hashToIndex = ListMap(passwords.zipWithIndex: _*)

  override def receive: Receive = {
    case SolvePassword(range: (Int, Int)) =>
      solvePassword(range)
    case MatchGeneSequence(range: (Int, Int)) =>
      matchGeneSequence(range)
    case CheckLinearCombination(range, crackedPasswords, targetSum) =>
      // println("Hello Linear Combination")
      checkLinearCombination(range, crackedPasswords, targetSum)
    case StartMining(personId, partnerId, prefix) =>
      startMining(personId, partnerId, prefix)
  }

  def solvePassword(range: (Int, Int)): Unit = {
    val passwordSet = passwords.toSet
    for (i <- range._1 to range._2) {
      val hashValue = sha256Hash(i.toString)
      if (passwordSet.contains(hashValue)) {
        masterActor ! PasswordResult(hashToIndex(hashValue) + 1, i)
      }
    }
  }

  def matchGeneSequence(range: (Int, Int)): Unit = {
    for (i <- range._1 to range._2) {
      // ToDo: big tree + single query vs. small tree + multiple queries
      val suffixTree = GeneralizedSuffixTree(geneSequences.patch(i, Nil, 1): _*)
      val test = suffixTree.findLongestCommonSubsequences(geneSequences(i))
      val resultSet = test.head._3
      val resultIndex = resultSet.toList.head._1
      val partnerId = resultIndex + 1 + (resultIndex >= i).compare(false)
      masterActor ! MatchingResult(i + 1, partnerId)
    }
  }

  def checkLinearCombination(range: (Long, Long), crackedPasswords: Array[Int], targetSum: Long): Unit = {
    var i: Long = range._1
    while (i < range._2) {
      i += 1
      val valueSum = crackedPasswords.indices.foldLeft(0L)((sum: Long, x: Int) => sum + ((i >> x) & 1) * crackedPasswords(x))
      if (valueSum == targetSum) {
        masterActor ! LinearCombinationResult(i)
        return
      }
    }
    masterActor ! NextTask()
  }

  def startMining(personId: Int, partnerId: Int, prefix: Int): Unit = {
    val prefixString = if (prefix == 1) "11111" else "00000"
    while (true) {
      val hash = sha256Hash((partnerId + Random.nextInt()).toString)
      if (hash.startsWith(prefixString)) {
        masterActor ! HashMiningResult(personId, hash)
        return
      }
    }
  }

}

object Worker {

  def sha256Hash(hash: String): String = {
    String.format("%064x", new BigInteger(1, java.security.MessageDigest
      .getInstance("SHA-256")
      .digest(hash.getBytes("UTF-8"))))
  }

  def props(passwords: Vector[String], geneSequences: Vector[String], masterActor: ActorSelection): Props = Props(new Worker(passwords, geneSequences, masterActor))

  final case class SolvePassword(range: (Int, Int)) extends Task

  final case class MatchGeneSequence(range: (Int, Int)) extends Task

  final case class CheckLinearCombination(range: (Long, Long), crackedPasswords: Array[Int], targetSum: Long) extends Task

  final case class StartMining(personId: Int, partnerId: Int, prefix: Int) extends Task
}
