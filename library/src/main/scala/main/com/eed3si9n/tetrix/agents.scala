package com.eed3si9n.tetrix

class Agent {
  def utility(state: GameState): Double =
    if (state.status == GameOver) -1000.0
    else state.lineCount.toDouble
  def bestMove(state: GameState): StageMessage = MoveLeft
}
