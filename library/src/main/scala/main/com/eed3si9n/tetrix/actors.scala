package com.eed3si9n.tetrix

import akka.actor._

sealed trait StageMessage
case object MoveLeft extends StageMessage
case object MoveRight extends StageMessage
case object RotateCW extends StageMessage
case object Tick extends StageMessage
case object Drop extends StageMessage
case object View extends StageMessage

class StageActor(s0: GameState) extends Actor {
  import Stage._

  private[this] var state: GameState = s0

  def receive = {
    case MoveLeft  => state = moveLeft(state)
    case MoveRight => state = moveRight(state)
    case RotateCW  => state = rotateCW(state)
    case Tick      => state = tick(state)
    case Drop      => state = drop(state)
    case View      => sender ! state.view
  }
}
