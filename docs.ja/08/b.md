---
out: pruning.html
---

### 探索木の刈り込み

> **刈り込み** (pruning) を用いることで最終的な選択肢に違いの出ない探索木の部分を無視することができる。

制御無しでは探索木は指数関数的に大きくなっていくため、この概念はここでも当てはまる。

1. `Drop` と `Tick` を抜くことで分岐数を 5 から 3 に減らせる。既に現在のピースを落とすことを仮定しているため、あとは `s0` も `Drop` 付きで評価すればいいだけだ。
2. 次に、現在のピースの 4つの全ての向きを事前に分岐させることで `RotateCW` も消える。ほとんどの場合は `RotateCW :: MoveLeft :: RotateCW :: Drop :: Nil` と `RotateCW :: RotateCW :: MoveLeft :: Drop :: Nil` は同じ状態に到達する。
3. 現在のピースを可能なかぎり左に寄せることで `MoveLeft` も抜くことができる。

ピースは 4つの向きと 9つの x位置を取ることができる。つまり、指数関数的な木の探索はこれで 36 という定数サイズで近似化できる。

`PieceKind` に基いて可能な向きの数を列挙する:

```scala
  private[this] def orientation(kind: PieceKind): Int = {
    case IKind => 2
    case JKind => 4
    case LKind => 4
    case OKind => 1
    case SKind => 2
    case TKind => 4
    case ZKind => 2
  }
```

次に、REPL を使って、ある状態からエージェントが何回右か左かに動かせるかを計算する。

```scala
scala> val s = newState(Nil, (10, 20), TKind :: TKind :: Nil)
s: com.eed3si9n.tetrix.GameState = GameState(List(
  Block((4,18),TKind), Block((5,18),TKind), Block((6,18),TKind), Block((5,19),TKind)),
  (10,20),Piece((5.0,18.0),TKind,List((-1.0,0.0), (0.0,0.0), (1.0,0.0), (0.0,1.0))),
  Piece((2.0,1.0),TKind,List((-1.0,0.0), (0.0,0.0), (1.0,0.0), (0.0,1.0))),List(),ActiveStatus,0)

scala> import scala.annotation.tailrec
import scala.annotation.tailrec

scala> @tailrec def leftLimit(n: Int, s: GameState): Int = {
          val next = moveLeft(s)
          if (next.currentPiece.pos == s.currentPiece.pos) n
          else leftLimit(n + 1, next)
       }
leftLimit: (n: Int, s: com.eed3si9n.tetrix.GameState)Int

scala> leftLimit(0, s)
res1: Int = 4
```

右の分も同じに作って、`sideLimit` メソッドの完成だ:

```scala
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
```

これで `actionSeqs` を作る準備が整った:

```scala
                                                                              s2"""
  ActionSeqs function should
    list out potential action sequences                                       \$actionSeqs1
                                                                              """
...
  def actionSeqs1 = {
    val s = newState(Nil, (10, 20), TKind :: TKind :: Nil)
    val seqs = agent.actionSeqs(s)
    seqs.size must_== 32
  }
```

スタブする:

```scala
  def actionSeqs(s0: GameState): Seq[Seq[StageMessage]] = Nil
```

予想通りテストは失敗する:

```
[info] ActionSeqs function should
[error] x list out potential action sequences
[error]    '0' is not equal to '32' (AgentSpec.scala:15)
```

これが実装となる:

```scala
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
```

REPL でアウトプットを見てみる:

```scala
scala> val s = newState(Nil, (10, 20), TKind :: TKind :: Nil)
s: com.eed3si9n.tetrix.GameState = GameState(List(Block((4,18),TKind), Block((5,18),TKind),
   Block((6,18),TKind), Block((5,19),TKind)),(10,20),
   Piece((5.0,18.0),TKind,List((-1.0,0.0), (0.0,0.0), (1.0,0.0), (0.0,1.0))),
   Piece((2.0,1.0),TKind,List((-1.0,0.0), (0.0,0.0), (1.0,0.0), (0.0,1.0))),List(),ActiveStatus,0)

scala> val agent = new Agent
agent: com.eed3si9n.tetrix.Agent = com.eed3si9n.tetrix.Agent@649f7367

scala> agent.actionSeqs(s)
res0: Seq[Seq[com.eed3si9n.tetrix.StageMessage]] = Vector(List(MoveLeft),
  List(MoveLeft, MoveLeft), List(MoveLeft, MoveLeft, MoveLeft),
  List(MoveLeft, MoveLeft, MoveLeft, MoveLeft), List(), List(MoveRight), 
  List(MoveRight, MoveRight), List(MoveRight, MoveRight, MoveRight), 
  List(RotateCW, MoveLeft), List(RotateCW, MoveLeft, MoveLeft), 
  List(RotateCW, MoveLeft, MoveLeft, MoveLeft), 
  List(RotateCW, MoveLeft, MoveLeft, MoveLeft, MoveLeft), List(RotateCW), 
  List(RotateCW, MoveRight), List(RotateCW, MoveRight, MoveRight), 
  List(RotateCW, MoveRight, MoveRight, MoveRight), List(RotateCW, RotateCW, MoveLeft), 
  List(RotateCW, RotateCW, MoveLeft, MoveLeft), 
  List(RotateCW, RotateCW, MoveLeft, MoveLeft, MoveLeft), 
  List(RotateCW, RotateCW, MoveLeft, MoveLeft, MoveLeft, MoveLeft), 
  List(RotateCW, RotateCW),...
```

アクション列の一つに、現在の状態を評価する `List()` があることに注意してほしい。全てのテストが通る:

```
[info] ActionSeqs function should
[info] + list out potential action sequences
```

`actionSeqs` を使って `bestMove` を書き換えよう:

```scala
  def bestMove(s0: GameState): StageMessage = {
    var retval: Seq[StageMessage] = Nil 
    var current: Double = minUtility
    actionSeqs(s0) foreach { seq =>
      val ms = seq ++ Seq(Drop)
      val u = utility(Function.chain(ms map {toTrans})(s0))
      if (u > current) {
        current = u
        retval = seq
      } // if
    }
    println("selected " + retval + " " + current.toString)
    retval.headOption getOrElse {Tick}
  }
```

スペックを加えよう。例えば `(0, 8)` にだけ一つ穴を開けておいて、解くのには回転が何回かと `MoveRight` が何個も必要な状態はどうだろう? 以前のエージェントだと多分解けなかったはずの問題だ。

```scala
                                                                              s2"""
  Solver should
    pick MoveLeft for s1                                                      \$solver1
    pick Drop for s3                                                          \$solver2
    pick RotateCW for s5                                                      \$solver3
                                                                              """
...
  def s5 = newState(Seq(
      (0, 0), (1, 0), (2, 0), (3, 0), (4, 0), (5, 0), (6, 0),
      (7, 0), (9, 0))
    map { Block(_, TKind) }, (10, 20), ttt)
  def solver3 =
    agent.bestMove(s5) must_== RotateCW
```

オールグリーン。次に、swing UI を走らせてみよう。

![day8b](http://eed3si9n.com/images/tetrix-in-scala-day8b.png)

```
[info] selected List(RotateCW, MoveLeft, MoveLeft, MoveLeft, MoveLeft) 1.4316304877998318
[info] selected List(MoveLeft, MoveLeft, MoveLeft, MoveLeft) 1.4316304877998318
[info] selected List(MoveLeft, MoveLeft, MoveLeft) 1.4316304877998318
[info] selected List(MoveLeft, MoveLeft) 1.4316304877998318
[info] selected List() 1.4108824377664941
```

動作がまだ近視眼的だけど、合理性の鱗片が見えてきたと思う。続きはまた明日。

```
\$ git fetch origin
\$ git co day8v2 -b try/day8
\$ sbt swing/run
```
