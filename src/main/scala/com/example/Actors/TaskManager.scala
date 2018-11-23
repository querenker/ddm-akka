package com.example.Actors

import akka.actor.{Actor, Props}

class TaskManager extends Actor {
  override def receive: Receive = {
    case msg: String => println(s"TaskManager: received message $msg")
  }
}

object TaskManager {
  def props(): Props = Props(new TaskManager())
}
