package com.eed3si9n.tetrix

class AbstractUI {
  import Stage._
  import java.{util => ju}

  private[this] var state = newState(Block((0, 0), TKind) :: Nil)
  private[this] val timer = new ju.Timer
  timer.scheduleAtFixedRate(new ju.TimerTask {
    def run { state = tick(state) }
  }, 0, 1000) 

  def left() {
    state = moveLeft(state)
  }
  def right() {
    state = moveRight(state)
  }
  def up() {
    state = rotateCW(state)
  }
  def down() {
    state = tick(state)
  }
  def space() {
  }
  def view: GameView = state.view
}
