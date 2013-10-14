---
out: heuristic-function.html
---

### ヒューリスティック関数

作戦B はヒューリスティック関数を導入することだ。

> *h(n)* = ノード *n* からゴールのノードまでの最も安価なコースにかかるコストの見積り

正確には僕らにはゴールとなるノードが無いため、この用語は当てはまらないかもしれないけど、概要としては現状を近似化して木探索を正しい方向につついてやることだ。この場合は、悪い陣形に対するペナルティを作ると考えることができる。例えば、列と列の間に 2つ以上の高さの差があることに対してペナルティを加えよう。差を自乗してペナルティが段階的に厳しくなっていくようにする。

```scala
                                                                              s2"""
    penalize having gaps between the columns                                  \$utility4
                                                                              """
...
  def utility4 = {
    val s = newState(Seq(
      (0, 0), (0, 1), (0, 2), (0, 3), (0, 4), (0, 5), (0, 6))
      map { Block(_, TKind) }, (10, 20), TKind :: TKind :: Nil)
    agent.utility(s) must_== -36.0
  }
```

期待通りテストは失敗する:

```
[info] Utility function should
[info] + evaluate initial state as 0.0,
[info] + evaluate GameOver as -1000.0,
[info] + evaluate an active state by lineCount
[error] x penalize having gaps between the columns
[error]    '0.0' is not equal to '-36.0' (AgentSpec.scala:10)
```

これは REPL を使って解いてみよう。sbt から `console` と打ち込む。

```scala
Welcome to Scala version 2.10.2 (Java HotSpot(TM) 64-Bit Server VM, Java 1.6.0_51).
Type in expressions to have them evaluated.
Type :help for more information.

scala> import com.eed3si9n.tetrix._
import com.eed3si9n.tetrix._

scala> import Stage._
import Stage._

scala> val s = newState(Seq(
                (0, 0), (0, 1), (0, 2), (0, 3), (0, 4), (0, 5), (0, 6))
                map { Block(_, TKind) }, (10, 20), TKind :: TKind :: Nil)
s: com.eed3si9n.tetrix.GameState = GameState(List(Block((0,0),TKind), Block((0,1),TKind),
  Block((0,2),TKind), Block((0,3),TKind), Block((0,4),TKind), Block((0,5),TKind),
  Block((0,6),TKind), Block((4,18),TKind), Block((5,18),TKind), Block((6,18),TKind),
  Block((5,19),TKind)),(10,20),
  Piece((5.0,18.0),TKind,List((-1.0,0.0), (0.0,0.0), (1.0,0.0), (0.0,1.0))),
  Piece((2.0,1.0),TKind,List((-1.0,0.0), (0.0,0.0), (1.0,0.0), (0.0,1.0))),
  List(),ActiveStatus,0)

scala> s.blocks map {_.pos} groupBy {_._1}
res0: scala.collection.immutable.Map[Int,Seq[(Int, Int)]] = Map(
  5 -> List((5,18), (5,19)), 4 -> List((4,18)), 6 -> List((6,18)),
  0 -> List((0,0), (0,1), (0,2), (0,3), (0,4), (0,5), (0,6)))
```

これはまずい。現在のピースが `s.blocks` に入り込んでいる。`unload` は `Stage` オブジェクト内のプレイベートなメソッドだ。`GameState` クラスにリファクタリングしてみる:

```scala
case class GameState(blocks: Seq[Block], gridSize: (Int, Int),
    currentPiece: Piece, nextPiece: Piece, kinds: Seq[PieceKind],
    status: GameStatus, lineCount: Int) {
  def view: GameView = ...
  def unload(p: Piece): GameState = {
    val currentPoss = p.current map {_.pos}
    this.copy(blocks = blocks filterNot { currentPoss contains _.pos })
  }
  def load(p: Piece): GameState =
    this.copy(blocks = blocks ++ p.current)
}
```

`Stage` を調整すると今のテスト意外は全て通過した。REPL に戻ろう:

```scala
... 上に同じ ...

scala> s.unload(s.currentPiece)
res0: com.eed3si9n.tetrix.GameState = GameState(List(Block((0,0),TKind),
  Block((0,1),TKind), Block((0,2),TKind), Block((0,3),TKind), Block((0,4),TKind),
  Block((0,5),TKind), Block((0,6),TKind)),(10,20),
  Piece((5.0,18.0),TKind,List((-1.0,0.0), (0.0,0.0), (1.0,0.0), (0.0,1.0))),
  Piece((2.0,1.0),TKind,List((-1.0,0.0), (0.0,0.0), (1.0,0.0), (0.0,1.0))),List(),ActiveStatus,0)

scala> s.unload(s.currentPiece).blocks map {_.pos} groupBy {_._1}
res1: scala.collection.immutable.Map[Int,Seq[(Int, Int)]] = Map(
  0 -> List((0,0), (0,1), (0,2), (0,3), (0,4), (0,5), (0,6)))

scala> val heights = s.unload(s.currentPiece).blocks map {_.pos} groupBy {_._1} map {
         case (k, v) => (k, v.map({_._2}).max) }
heights: scala.collection.immutable.Map[Int,Int] = Map(0 -> 6)

scala> heights.getOrElse(1, 0)
res6: Int = 0

scala> (0 to s.gridSize._1 - 2)
res7: scala.collection.immutable.Range.Inclusive = Range(0, 1, 2, 3, 4, 5, 6, 7, 8)

scala> val gaps = (0 to s.gridSize._1 - 2).toSeq map { x =>
         heights.getOrElse(x, 0) - heights.getOrElse(x + 1, 0) }
gaps: scala.collection.immutable.IndexedSeq[Int] = Vector(6, 0, 0, 0, 0, 0, 0, 0, 0)

scala> val gaps = (0 to s.gridSize._1 - 2).toSeq map { x => 
         heights.getOrElse(x, 0) - heights.getOrElse(x + 1, 0) } filter {_ > 1}
gaps: scala.collection.immutable.IndexedSeq[Int] = Vector(6)

scala> gaps map {x => x * x} sum
res5: Int = 36
```

実際には上にあるよりも多くの打ち間違いや実験を行った。だけど、何をやってるかは分ってもらえると思う。REPL を使って段階的に演算をつなげていくことで式を構築することができる。答が得られたらそれをエディタにコピペすればいい:

```scala
  def utility(state: GameState): Double =
    if (state.status == GameOver) minUtility
    else state.lineCount.toDouble - penalty(state)
  private[this] def penalty(s: GameState): Double = {
    val heights = s.unload(s.currentPiece).blocks map {_.pos} groupBy {
      _._1} map { case (k, v) => (k, v.size) }
    val gaps = (0 to s.gridSize._1 - 2).toSeq map { x =>
      heights.getOrElse(x, 0) - heights.getOrElse(x + 1, 0) } filter {_ > 1}
    gaps map {x => x * x} sum
  }
```

テストは通過するようになった。

```
[info]   Utility function should
[info]     + evaluate initial state as 0.0,
[info]     + evaluate GameOver as -1000.0,
[info]     + evaluate an active state by lineCount
[info]     + penalize having gaps between the columns
```

現在のソルバーにはもう一つ問題がある。`Drop` 以外は現在のピースが空中に浮かんでいるため評価の対象にならないということだ。これを解決するには既に落ちていなければ `Drop` を後ろに追加すればいい。これはまず実装を変えてみてどのテストが失敗するかみてみよう:

```scala
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
    retval
  }
```

`Function.chain` を使った遷移関数の合成が再び出てきた。じゃあ、テストを走らせてみよう。

```
[info] Solver should
[info] + pick MoveLeft for s1
[error] x pick Drop for s3
[error]    'Tick' is not equal to 'Drop' (AgentSpec.scala:14)
```

これは驚くことではない。`Drop` を最後に追加したため、`Tick` と `Drop` の違いが無くなったというだけだ。スペックを緩和して直す:

```scala
  def solver2 =
    agent.bestMove(s3) must beOneOf(Drop, Tick)
```

これでエージェントは `MoveLeft` 以外の動作も選びはじめたけども、まだグリッドの左にかなり偏っている。

![day7b](../files/tetrix-in-scala-day7b.png)

探索木を深くすればましになってくれると思う。続きはまた明日。

```
\$ git fetch origin
\$ git co day7v2 -b try/day7
\$ sbt swing/run
```
