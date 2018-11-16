package com.example.Actors

import java.math.BigInteger

import akka.actor.{Actor, Props}

class PasswordSolverWorker(passwords: List[String]) extends Actor {

  import PasswordSolverSupervisor._
  import PasswordSolverWorker._

  override def receive: Receive = {
    case SolvePassword(range: (Int, Int)) =>
      solvePassword(range)
  }

  def solvePassword(range: (Int, Int)): Unit = {
    val passwordSet = passwords.toSet
    for (i <- range._1 to range._2) {
      val DEBUG = 1
      val hashValue = sha256Hash(i.toString)
      if (passwordSet.contains(hashValue)) {
        context.parent ! PasswordResult(hashValue, i.toString)
      }
    }
  }
}

object PasswordSolverWorker {
  def props(passwords: List[String]): Props = Props(new PasswordSolverWorker(passwords))

  final case class SolvePassword(range: (Int, Int))

  def sha256Hash(hash: String): String = {
    String.format("%064x", new BigInteger(1, java.security.MessageDigest
      .getInstance("SHA-256")
      .digest(hash.getBytes("UTF-8"))))
  }

}