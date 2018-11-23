package com.example.Actors

import java.math.BigInteger

import akka.actor.{Actor, ActorSelection, Props}
import com.example.Actors.Supervisor.{MatchingResult, PasswordResult}
import com.example.Actors.TaskManager.RegisterWorker
import com.example.Actors.Worker._
import gstlib.GeneralizedSuffixTree

class Worker(passwords: Vector[String], geneSequences: Vector[String], masterActor: ActorSelection) extends Actor {

  override def preStart(): Unit = {
    masterActor ! RegisterWorker()
  }

  override def receive: Receive = {
    case SolvePassword(range: (Int, Int)) =>
      solvePassword(range)
    case MatchGeneSequence(range: (Int, Int)) =>
      matchGeneSequence(range)
  }

  def solvePassword(range: (Int, Int)): Unit = {
    val passwordSet = passwords.toSet
    for (i <- range._1 to range._2) {
      val hashValue = sha256Hash(i.toString)
      if (passwordSet.contains(hashValue)) {
        context.parent ! PasswordResult(hashValue, i.toString)
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
      context.parent ! MatchingResult(i + 1, partnerId)
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

  final case class SolvePassword(range: (Int, Int))

  final case class MatchGeneSequence(range: (Int, Int))
}
