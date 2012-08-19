package com.eed3si9n.tetrix

import akka.actor._
import akka.util.duration._
import akka.pattern.ask
import akka.dispatch.{Future, Await}

sealed trait StateMessage
case object GetState extends StateMessage
case class SetState(s: GameState) extends StateMessage
case object GetView extends StateMessage

class StateActor(s0: GameState) extends Actor {
  private[this] var state: GameState = s0
  
  def receive = {
    case GetState    => sender ! state
    case SetState(s) => state = s
    case GetView     => sender ! state.view
  }
}

sealed trait StageMessage
case object MoveLeft extends StageMessage
case object MoveRight extends StageMessage
case object RotateCW extends StageMessage
case object Tick extends StageMessage
case object Drop extends StageMessage
case object Attack extends StageMessage

class StageActor(stateActor: ActorRef) extends Actor {
  import Stage._

  def receive = {
    case MoveLeft  => updateState {moveLeft}
    case MoveRight => updateState {moveRight}
    case RotateCW  => updateState {rotateCW}
    case Tick      => updateState {tick}
    case Drop      => updateState {drop}
    case Attack    => updateState {notifyAttack}
  }
  private[this] def opponent: ActorRef =
    if (self.path.name == "stageActor1") context.actorFor("/user/stageActor2")
    else context.actorFor("/user/stageActor1")
  private[this] def updateState(trans: GameState => GameState) {
    val future = (stateActor ? GetState)(1 second).mapTo[GameState]
    val s1 = Await.result(future, 1 second)
    val s2 = trans(s1)
    stateActor ! SetState(s2)
    (0 to s2.lastDeleted - 2) foreach { i =>
      opponent ! Attack
    }
  }
}

sealed trait AgentMessage
case class BestMove(s: GameState) extends AgentMessage

class AgentActor(stageActor: ActorRef) extends Actor {
  private[this] val agent = new Agent

  def receive = {
    case BestMove(s: GameState) =>
      val message = agent.bestMove(s)
      if (message == Drop) stageActor ! Tick
      else stageActor ! message 
  }
}

sealed trait GameMasterMessage
case object Start

class GameMasterActor(stateActor1: ActorRef, stateActor2: ActorRef,
    agentActor: ActorRef) extends Actor {
  def receive = {
    case Start => loop 
  }
  private[this] def loop {
    val minActionTime = 337
    var s = getStatesAndJudge._2
    while (s.status == ActiveStatus) {
      val t0 = System.currentTimeMillis
      agentActor ! BestMove(getState2)
      val t1 = System.currentTimeMillis
      if (t1 - t0 < minActionTime) Thread.sleep(minActionTime - (t1 - t0))
      s = getStatesAndJudge._2
    }
  }
  private[this] def getStatesAndJudge: (GameState, GameState) = {
    var s1 = getState1
    var s2 = getState2
    if (s1.status == GameOver && s2.status != Victory) {
      stateActor2 ! SetState(s2.copy(status = Victory))
      s2 = getState2
    }
    if (s1.status != Victory && s2.status == GameOver) {
      stateActor1 ! SetState(s1.copy(status = Victory))
      s1 = getState1
    }
    (s1, s2)
  }
  private[this] def getState1: GameState = {
    val future = (stateActor1 ? GetState)(1 second).mapTo[GameState]
    Await.result(future, 1 second)
  }
  private[this] def getState2: GameState = {
    val future = (stateActor2 ? GetState)(1 second).mapTo[GameState]
    Await.result(future, 1 second)
  }
}
