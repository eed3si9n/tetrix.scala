package com.eed3si9n.tetrix

class Agent {
  import Stage._
  import scala.annotation.tailrec

  private[this] val minUtility = -1000.0 
  def utility(state: GameState): Double =
    if (state.status == GameOver) minUtility
    else reward(state) - penalty(state) / 10.0
  def reward(s: GameState): Double = s.lineCount.toDouble
  def penalty(s: GameState): Double = {
    val groupedByX = s.unload(s.currentPiece).blocks map {_.pos} groupBy {_._1}
    val heights = groupedByX map { case (k, v) => v.map({_._2 + 1}).max }
    val coverups = groupedByX flatMap { case (k, vs) => 
      vs.map(_._2).sorted.zipWithIndex.dropWhile(x => x._1 == x._2).map(_._1 + 1) }
    math.sqrt( (heights ++ coverups) map { x => x * x } sum)
  }
  def bestMove(s0: GameState): StageMessage = {
    var retval: Seq[StageMessage] = Nil 
    var current: Double = minUtility
    stopWatch("bestMove") {
      val nodes = actionSeqs(s0) map { seq =>
        val ms = seq ++ Seq(Drop)
        val s1 = Function.chain(ms map {toTrans})(s0)
        val u = utility(s1)
        if (u > current) {
          current = u
          retval = seq
        } // if
        SearchNode(s1, ms, u)
      }
      nodes foreach { node =>
        actionSeqs(node.state) foreach { seq =>
          val ms = seq ++ Seq(Drop)
          val s2 = Function.chain(ms map {toTrans})(node.state)
          val u = utility(s2)
          if (u > current) {
            current = u
            retval = node.actions ++ seq
          } // if
        }
      }
    } // stopWatch
    println("selected " + retval + " " + current.toString)
    retval.headOption getOrElse {Drop}
  }
  case class SearchNode(state: GameState, actions: Seq[StageMessage], score: Double)

  def actionSeqs(s0: GameState): Seq[Seq[StageMessage]] = {
    val rotationSeqs: Seq[Seq[StageMessage]] =
      (0 to orientation(s0.currentPiece.kind) - 1).toSeq map { x =>
        Nil padTo (x, RotateCW)
      }
    val translationSeqs: Seq[Seq[StageMessage]] =
      sideLimit(s0) match {
        case (l, r) =>
          ((1 to l).toSeq map { x =>
            Nil padTo (x, MoveLeft)
          }) ++
          Seq(Nil) ++
          ((1 to r).toSeq map { x =>
            Nil padTo (x, MoveRight)
          })
      }
    for {
      r <- rotationSeqs
      t <- translationSeqs
    } yield r ++ t
  }
  private[this] def toTrans(message: StageMessage): GameState => GameState =
    message match {
      case MoveLeft  => moveLeft
      case MoveRight => moveRight
      case RotateCW  => rotateCW
      case Tick      => tick
      case Drop      => drop 
    }
  private[this] def orientation(kind: PieceKind): Int = kind match {
    case IKind => 2
    case JKind => 4
    case LKind => 4
    case OKind => 1
    case SKind => 2
    case TKind => 4
    case ZKind => 2
  }
  private[this] def sideLimit(s0: GameState): (Int, Int) = {
    @tailrec def leftLimit(n: Int, s: GameState): Int = {
      val next = moveLeft(s)
      if (next.currentPiece.pos == s.currentPiece.pos) n
      else leftLimit(n + 1, next)
    }
    @tailrec def rightLimit(n: Int, s: GameState): Int = {
      val next = moveRight(s)
      if (next.currentPiece.pos == s.currentPiece.pos) n
      else rightLimit(n + 1, next)
    }
    (leftLimit(0, s0), rightLimit(0, s0))
  }
  private[this] def stopWatch[A](name: String)(arg: => A): A = {
    val t0 = System.currentTimeMillis
    val retval: A = arg
    val t1 = System.currentTimeMillis
    println(name + " took " + (t1 - t0).toString + " ms")
    retval
  }
}
