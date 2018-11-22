package com.example.Actors

import akka.actor.{Actor, Props}

class Worker extends Actor {

  override def preStart(): Unit = {
    println("Hello from remote")
    val remoteActor = context.actorSelection("akka.tcp://MasterSystem@127.0.0.1:5150/user/taskManager")
    remoteActor ! "Hello World"
  }

  override def receive: Receive = {
    case msg: String => println(s"Worker: received message $msg")
  }
}

object Worker {

  def props(): Props = Props(new Worker())
}
