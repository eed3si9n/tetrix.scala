---
out: buggy-penalty.html
---

### バギーなペナルティ

第二の疑惑はペナルティの計算が何かおかしいということだ。テストを加えてみよう:

```scala
    """penalize having gaps between the columns"""          ! utility4^
...
  def utility4 = {
    val s = newState(Seq(
      (0, 0), (0, 1), (0, 2), (0, 3), (0, 4), (0, 5), (0, 6))
      map { Block(_, TKind) }, (10, 20), TKind :: TKind :: Nil)
    agent.utility(s) must_== -36.0 
  } and {
    val s = newState(Seq((1, 0), (1, 1), (2, 1), (2, 2))
    map { Block(_, ZKind) }, (10, 20), TKind :: TKind :: Nil)
    agent.utility(s) must_== -13.0
  }
```

思った通り失敗する。

```
[error] x penalize having gaps between the columns
[error]    '-4.0' is not equal to '-13.0' (AgentSpec.scala:35)
```

REPL に入る前に、何度も同じ事を打ち込まなくてもいいように以下を `build.scala` に加える:

```scala
initialCommands in console := """import com.eed3si9n.tetrix._
                                |import Stage._""".stripMargin,
```

再読み込みした後で sbt シェルから `console` と打って REPL を起動する:

```scala
[info] 
import com.eed3si9n.tetrix._
import Stage._
Welcome to Scala version 2.10.2 (Java HotSpot(TM) 64-Bit Server VM, Java 1.6.0_51).
Type in expressions to have them evaluated.
Type :help for more information.

scala> move // タブキーを押す
moveLeft    moveRight 
```

そう、REPL からはタブ補完まで使える。

```scala
scala>     val s = newState(Seq((1, 0), (1, 1), (2, 1), (2, 2))
          map { Block(_, ZKind) }, (10, 20), TKind :: TKind :: Nil)
s: com.eed3si9n.tetrix.GameState = GameState(List(Block((1,0),ZKind), Block((1,1),ZKind),
  Block((2,1),ZKind), Block((2,2),ZKind), Block((4,18),TKind), Block((5,18),TKind),
  Block((6,18),TKind), Block((5,19),TKind)),(10,20),
  Piece((5.0,18.0),TKind,List((-1.0,0.0), (0.0,0.0), (1.0,0.0), (0.0,1.0))),
  Piece((2.0,1.0),TKind,List((-1.0,0.0), (0.0,0.0), (1.0,0.0), (0.0,1.0))),List(),ActiveStatus,0)

scala>     val heights = s.unload(s.currentPiece).blocks map {
             _.pos} groupBy {_._1} map { case (k, v) => (k, v.map({_._2}).max) }
heights: scala.collection.immutable.Map[Int,Int] = Map(1 -> 1, 2 -> 2)

scala>     val gaps = (0 to s.gridSize._1 - 2).toSeq map { x =>
             heights.getOrElse(x, 0) - heights.getOrElse(x + 1, 0) } filter {_ > 1}
gaps: scala.collection.immutable.IndexedSeq[Int] = Vector(2)
```

Off-by-one エラーだ! 気付いたかな? 一番下の座標が `0` なのにデフォルトで `0` を返している。

あとそれから負の数もフィルター漏れしている。正しい `gap` はこれだ:

```scala
scala>     val gaps = (0 to s.gridSize._1 - 2).toSeq map { x =>
             heights.getOrElse(x, -1) - heights.getOrElse(x + 1, -1) } filter {math.abs(_) > 1}
gaps: scala.collection.immutable.IndexedSeq[Int] = Vector(-2, 3)
```

手で計算した -36.0 まで間違っていたためこのバグに気付かなかった。これで全てのテストが通るようになった:

```
[info] + penalize having gaps between the columns
```

少しは論理的になったけど、ペナルティは実際のゲームをスコアや正しいセットアップに導いていない気がする。まず、効用関数の報酬部分とペナルティ部分を分けてテストしやすいようにする。次に、高低差の代わりに高さそのものにペナルティを課してみよう。

```scala
                                                                              s2"""
  Penalty function should
    penalize having blocks stacked up high                                    \$penalty1
                                                                              """
...
  def penalty1 = {
    val s = newState(Seq(
      (0, 0), (0, 1), (0, 2), (0, 3), (0, 4), (0, 5), (0, 6))
      map { Block(_, TKind) }, (10, 20), TKind :: TKind :: Nil)
    agent.penalty(s) must_== 49.0 
  } and {
    val s = newState(Seq((1, 0))
    map { Block(_, ZKind) }, (10, 20), TKind :: TKind :: Nil)
    agent.penalty(s) must_== 1.0
  }
```

これが新しい `penalty` だ:

```scala
  def penalty(s: GameState): Double = {
    val heights = s.unload(s.currentPiece).blocks map {
      _.pos} groupBy {_._1} map { case (k, v) => v.map({_._2 + 1}).max }
    heights map { x => x * x } sum
  }
```

報酬とペナルティのバランスも調整したい。現在はペナルティの回避に比べてラインを消すインセンティブが少なすぎる。

```scala
  def utility(state: GameState): Double =
    if (state.status == GameOver) minUtility
    else reward(state) - penalty(state) / 10.0
```

以下のゲームではやっと一つのラインを消すことができた:

![day8](http://eed3si9n.com/images/tetrix-in-scala-day8.png)
