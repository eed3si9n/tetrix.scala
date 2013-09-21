package com.eed3si9n.tetrix

object Stage {
  def newState(blocks: Seq[Block]): GameState = {
    val size = (10, 20)
    val dummy = Piece((0, 0), TKind)
    spawn(GameState(blocks, size, dummy))
  }
  val moveLeft  = transit { _.moveBy(-1.0, 0.0) }
  val moveRight = transit { _.moveBy(1.0, 0.0) }
  val rotateCW  = transit { _.rotateBy(-math.Pi / 2.0) }
  val tick = transit(_.moveBy(0.0, -1.0), spawn)
  
  private[this] def spawn(s: GameState): GameState = {
    def dropOffPos = (s.gridSize._1 / 2.0, s.gridSize._2 - 3.0)
    val p = Piece(dropOffPos, TKind)
    s.copy(blocks = s.blocks ++ p.current,
      currentPiece = p)
  }
  private[this] def transit(trans: Piece => Piece,
      onFail: GameState => GameState = identity): GameState => GameState =
    (s: GameState) => validate(s.copy(
        blocks = unload(s.currentPiece, s.blocks),
        currentPiece = trans(s.currentPiece))) map { case x =>
      x.copy(blocks = load(x.currentPiece, x.blocks))
    } getOrElse {onFail(s)}
  private[this] def validate(s: GameState): Option[GameState] = {
    val size = s.gridSize
    def inBounds(pos: (Int, Int)): Boolean =
      (pos._1 >= 0) && (pos._1 < size._1) && (pos._2 >= 0) && (pos._2 < size._2)
    val currentPoss = s.currentPiece.current map {_.pos}
    if ((currentPoss forall inBounds) && 
      (s.blocks map {_.pos} intersect currentPoss).isEmpty) Some(s)
    else None
  }
  private[this] def unload(p: Piece, bs: Seq[Block]): Seq[Block] = {
    val currentPoss = p.current map {_.pos}
    bs filterNot { currentPoss contains _.pos  }
  }
  private[this] def load(p: Piece, bs: Seq[Block]): Seq[Block] =
    bs ++ p.current
}
