package com.eed3si9n.tetrix

class AbstractUI {
  private[this] val stage = new Stage((10, 20))
  def left() {
    stage.moveLeft()
  }
  def right() {
    stage.moveRight()
  }
  def up() {
  }
  def down() {
  }
  def space() {
  }
  def view: GameView = stage.view
}
