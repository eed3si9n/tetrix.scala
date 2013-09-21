package com.eed3si9n.tetrix

class Agent {
  import Stage._

  private[this] val minUtility = -1000.0 
  def utility(state: GameState): Double =
    if (state.status == GameOver) minUtility
    else state.lineCount.toDouble - penalty(state)
  private[this] def penalty(s: GameState): Double = {
    val heights = s.unload(s.currentPiece).blocks map {
      _.pos} groupBy {_._1} map { case (k, v) => (k, v.map({_._2}).max) }
    val gaps = (0 to s.gridSize._1 - 2).toSeq map { x =>
      heights.getOrElse(x, 0) - heights.getOrElse(x + 1, 0) } filter {_ > 1}
    gaps map {x => x * x} sum
  }
  def bestMove(s0: GameState): StageMessage = {
    var retval: StageMessage = MoveLeft 
    var current: Double = minUtility
    possibleMoves foreach { move =>
      val ms = 
        if (move == Drop) move :: Nil
        else move :: Drop :: Nil 
      val u = utility(Function.chain(ms map {toTrans})(s0))
      if (u > current) {
        current = u
        retval = move 
      } // if
    }
    println("selected " + retval + " " + current.toString)
    retval
  }
  private[this] val possibleMoves: Seq[StageMessage] =
    Seq(MoveLeft, MoveRight, RotateCW, Tick, Drop)
  private[this] def toTrans(message: StageMessage): GameState => GameState =
    message match {
      case MoveLeft  => moveLeft
      case MoveRight => moveRight
      case RotateCW  => rotateCW
      case Tick      => tick
      case Drop      => drop 
    }
}
