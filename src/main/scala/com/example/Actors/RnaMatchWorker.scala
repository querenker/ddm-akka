package com.example.Actors

import akka.actor.{Actor, Props}
import com.example.Actors.RnaMatchSupervisor.MatchingResult
import com.example.Actors.RnaMatchWorker.MatchGeneSequence
import gstlib.GeneralizedSuffixTree

class RnaMatchWorker(geneSequences: Vector[String]) extends Actor {

  override def receive: Receive = {
    case MatchGeneSequence(range: (Int, Int)) =>
      matchGeneSequence(range)
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

object RnaMatchWorker {

  def props(geneSequences: Vector[String]): Props = Props(new RnaMatchWorker(geneSequences))

  final case class MatchGeneSequence(range: (Int, Int))
}
