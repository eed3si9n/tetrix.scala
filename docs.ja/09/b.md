---
out: cavity.html
---

### 虫歯

エージェントがゲームをプレイするのを見てて思うのが、虫歯を作るのを何とも思っていないということだ。埋まってないスポットの上に 1つまたは複数のブロックがある状態のことを虫歯と呼んでいる。

```
-fig 1----


      xxxx
xxxx x xxx 
x x xxxxxx 
----------
```

高さによるペナルティを回避するために、例えば凸凹の表面に I字のバーを垂直に落とすのではなく、平らに寝かせたりする。僕としては虫歯を最小化して以下のようにプレイしてほしい:

```
-fig 2-----

   x 
   x  xxxx
   x x xxx 
x xxxxxxxx 
----------
```

最初の 4列に注目して高さによるペナルティを計算してみよう。

```scala
scala> val fig1 = math.sqrt(List(2, 2, 2, 2) map { x => x * x } sum)
fig1: Double = 4.0

scala> val fig2 = math.sqrt(List(1, 0, 4, 1) map { x => x * x } sum)
fig2: Double = 4.242640687119285
```

予想通り fig1 の方がペナルティが低くなる。別のブロックを覆っている全てのブロックに対しても、高さ*高さのべナルティを課そう。新しいペナルティは以下のとおり:

```scala
scala> val fig1b = math.sqrt(List(2, 2, 2, 2, 2, 2) map { x => x * x } sum)
fig1b: Double = 4.898979485566356

scala> val fig2b = math.sqrt(List(1, 0, 4, 1) map { x => x * x } sum)
fig2b: Double = 4.242640687119285
```

今回は fig2 の方が優先される。スペックに書いてみよう:

```scala
                                                                              s2"""
  Penalty function should
    penalize having blocks stacked up high                                    \$penalty1
    penalize having blocks covering other blocks                              \$penalty2
                                                                              """
...
  def penalty2 = {
    val s = newState(Seq(
      (0, 0), (2, 0), (0, 1), (1, 1), (2, 1), (3, 1))
      map { Block(_, TKind) }, (10, 20), TKind :: TKind :: Nil)
    agent.penalty(s) must beCloseTo(4.89, 0.01) 
  }
```

期待通りテストは失敗する:

```
[info] Penalty function should
[info] + penalize having blocks stacked up high
[error] x penalize having blocks covering other blocks
[error]    4.0 is not close to 4.89 +/- 0.01 (AgentSpec.scala:13)
```

REPL を使って実装する:

```scala
scala>     val s = newState(Seq(
             (0, 0), (2, 0), (0, 1), (1, 1), (2, 1), (3, 1))
             map { Block(_, TKind) }, (10, 20), TKind :: TKind :: Nil)
s: com.eed3si9n.tetrix.GameState = GameState(List(Block((0,0),TKind), Block((2,0),TKind),
  Block((0,1),TKind), Block((1,1),TKind), Block((2,1),TKind), Block((3,1),TKind), 
  Block((4,18),TKind), Block((5,18),TKind), Block((6,18),TKind), Block((5,19),TKind)),(10,20),
  Piece((5.0,18.0),TKind,List((-1.0,0.0), (0.0,0.0), (1.0,0.0), (0.0,1.0))),
  Piece((2.0,1.0),TKind,List((-1.0,0.0), (0.0,0.0), (1.0,0.0), (0.0,1.0))),List(),ActiveStatus,0)

scala> val groupedByX = s.unload(s.currentPiece).blocks map {_.pos} groupBy {_._1}
groupedByX: scala.collection.immutable.Map[Int,Seq[(Int, Int)]] = Map(
  3 -> List((3,1)), 1 -> List((1,1)), 2 -> List((2,0), (2,1)), 0 -> List((0,0), (0,1)))

scala> groupedByX map { case (k, vs) => vs.map(_._2).sorted.zipWithIndex }
res14: scala.collection.immutable.Iterable[Seq[(Int, Int)]] = List(
  List((1,0)), List((1,0)), List((0,0), (1,1)), List((0,0), (1,1)))

scala> groupedByX map { case (k, vs) =>
         vs.map(_._2).sorted.zipWithIndex.dropWhile(x => x._1 == x._2) }
res15: scala.collection.immutable.Iterable[Seq[(Int, Int)]] = List(
  List((1,0)), List((1,0)), List(), List())

scala> val coverups = groupedByX flatMap { case (k, vs) =>
         vs.map(_._2).sorted.zipWithIndex.dropWhile(x => x._1 == x._2).map(_._1 + 1) }
coverups: scala.collection.immutable.Iterable[Int] = List(2, 2)
```

できあがった `penalty` だ:

```scala
  def penalty(s: GameState): Double = {
    val groupedByX = s.unload(s.currentPiece).blocks map {_.pos} groupBy {_._1}
    val heights = groupedByX map { case (k, v) => v.map({_._2 + 1}).max }
    val coverups = groupedByX flatMap { case (k, vs) => 
      vs.map(_._2).sorted.zipWithIndex.dropWhile(x => x._1 == x._2).map(_._1 + 1) }
    math.sqrt( (heights ++ coverups) map { x => x * x } sum)
  }
```

これでテストが通る:

```scala
[info]   Penalty function should
[info]     + penalize having blocks stacked up high
[info]     + penalize having blocks covering other blocks
```

ゲームをプレイさせてみよう:

![day9c](http://eed3si9n.com/images/tetrix-in-scala-day9c.png)

虫歯の回避はうまくいったが、効きすぎているかもしれない。明日に段差のペナルティを再び導入する必要があるかもしれない。

```
\$ git fetch origin
\$ git co day9v2 -b try/day9
\$ sbt swing/run
```
