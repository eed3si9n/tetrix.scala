package com.eed3si9n.tetrix

class AbstractUI {
  import Stage._
  import java.{util => ju}
  import scala.collection.immutable.Stream

  private[this] var state = newState(Block((0, 0), TKind) :: Nil,
    randomStream(new util.Random))
  private[this] val timer = new ju.Timer
  timer.scheduleAtFixedRate(new ju.TimerTask {
    def run { state = tick(state) }
  }, 0, 1000)
  private[this] def randomStream(random: util.Random): Stream[PieceKind] =
    PieceKind(random.nextInt % 7) #:: randomStream(random)
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
    state = drop(state)
  }
  def view: GameView = state.view
}
