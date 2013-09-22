package com.eed3si9n.tetrix

import akka.actor._
import scala.concurrent.duration._
import akka.pattern.ask
import scala.concurrent.{Future, Await}

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

class StageActor(stateActor: ActorRef) extends Actor {
  import Stage._

  def receive = {
    case MoveLeft  => updateState {moveLeft}
    case MoveRight => updateState {moveRight}
    case RotateCW  => updateState {rotateCW}
    case Tick      => updateState {tick}
    case Drop      => updateState {drop}
  }

  private[this] def updateState(trans: GameState => GameState) {
    val future = (stateActor ? GetState)(1 second).mapTo[GameState]
    val s1 = Await.result(future, 1 second)
    val s2 = trans(s1)
    stateActor ! SetState(s2)
  }
}

sealed trait AgentMessage
case class BestMove(s: GameState) extends AgentMessage

class AgentActor(stageActor: ActorRef) extends Actor {
  private[this] val agent = new Agent

  def receive = {
    case BestMove(s: GameState) =>
      val message = agent.bestMove(s)
      if (message != Drop) stageActor ! message
  }
}

class GameMasterActor(stateActor: ActorRef, agentActor: ActorRef) extends Actor {
  def receive = {
    case Tick => 
      val s = getState
      if (s.status != GameOver) {
        agentActor ! BestMove(getState)
      } 
  }

  private[this] def getState: GameState = {
    val future = (stateActor ? GetState)(1 second).mapTo[GameState]
    Await.result(future, 1 second)
  } 
}
