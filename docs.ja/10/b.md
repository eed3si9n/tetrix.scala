---
out: crevasse.html
---

### クレバス

幅が 1 の段差は問題が多い。深さ 2 のクレバスは `J`、`L`、もしくは `I` を使って救う必要がある。深さ 3 と 4 は `I` だけだ。

    ----------

     x
     x 
     x
     x
    xx
    ----------

上の図の現行のペナルティを計算しよう:

```scala
scala> val fig = math.sqrt(List(1, 5) map { x => x * x } sum)
fig: Double = 5.0990195135927845
```

深さ3以上のクレバスは 4 * 高さ * 高さのペナルティを課すべきだ:

```scala
scala> val fig = math.sqrt(List(1, 5, 10) map { x => x * x } sum)
fig: Double = 11.224972160321824
```

スペックにしてみる:

```scala
                                                                              s2"""
    penalize having blocks creating deep crevasses                            \$penalty3
                                                                              """
...
  def penalty3 = {
    val s = newState(Seq(
      (0, 0), (1, 0), (1, 1), (1, 2), (1, 3), (1, 4))
      map { Block(_, TKind) }, (10, 20), TKind :: TKind :: Nil)
    agent.penalty(s) must beCloseTo(11.22, 0.01) 
  }
```

期待通りテストは失敗する:

```
[info] Penalty function should
[info] + penalize having blocks stacked up high
[info] + penalize having blocks covering other blocks
[error] x penalize having blocks creating deep crevasses
[error]    5.0990195135927845 is not close to 11.22 +/- 0.01 (AgentSpec.scala:14)
```

REPL へ:

```scala
scala>     val s = newState(Seq(
             (0, 0), (1, 0), (1, 1), (1, 2), (1, 3), (1, 4))
             map { Block(_, TKind) }, (10, 20), TKind :: TKind :: Nil)
s: com.eed3si9n.tetrix.GameState = GameState(List(Block((0,0),TKind),
  Block((1,0),TKind), Block((1,1),TKind), Block((1,2),TKind), Block((1,3),TKind),
  Block((1,4),TKind), Block((4,18),TKind), Block((5,18),TKind), Block((6,18),TKind),
  Block((5,19),TKind)),(10,20),
  Piece((5.0,18.0),TKind,List((-1.0,0.0), (0.0,0.0), (1.0,0.0), (0.0,1.0))),
  Piece((2.0,1.0),TKind,List((-1.0,0.0), (0.0,0.0), (1.0,0.0), (0.0,1.0))),List(),ActiveStatus,0)

val groupedByX = s.unload(s.currentPiece).blocks map {_.pos} groupBy {_._1}

scala> val heights = groupedByX map { case (k, v) => (k, v.map({_._2 + 1}).max) }
heights: scala.collection.immutable.Map[Int,Int] = Map(1 -> 5, 0 -> 1)

scala> val hWithDefault = heights withDefault { x =>
         if (x < 0 || x > s.gridSize._1 - 1) s.gridSize._2
         else 0
       }
hWithDefault: scala.collection.immutable.Map[Int,Int] = Map(1 -> 5, 0 -> 1)

scala> (-1 to s.gridSize._1 - 1) map { x => hWithDefault(x + 1) - hWithDefault(x) }
res2: scala.collection.immutable.IndexedSeq[Int] = Vector(-19, 4, -5, 0, 0, 0, 0, 0, 0, 0, 20)

scala> (-1 to s.gridSize._1 - 2) map { x =>
         val down = hWithDefault(x + 1) - hWithDefault(x)
         val up = hWithDefault(x + 2) - hWithDefault(x + 1)
         if (down < -2 && up > 2) math.min(hWithDefault(x), hWithDefault(x + 2))
         else 0
       }
res3: scala.collection.immutable.IndexedSeq[Int] = Vector(5, 0, 0, 0, 0, 0, 0, 0, 0, 0)
```

これが変更されたペナルティだ:

```scala
  def penalty(s: GameState): Double = {
    val groupedByX = s.unload(s.currentPiece).blocks map {_.pos} groupBy {_._1}
    val heights = groupedByX map { case (k, v) => (k, v.map({_._2 + 1}).max) }
    val hWithDefault = heights withDefault { x =>
      if (x < 0 || x > s.gridSize._1 - 1) s.gridSize._2
      else 0
    }
    val crevasses = (-1 to s.gridSize._1 - 2) flatMap { x =>
      val down = hWithDefault(x + 1) - hWithDefault(x)
      val up = hWithDefault(x + 2) - hWithDefault(x + 1)
      if (down < -2 && up > 2) Some(math.min(2 * hWithDefault(x), 2 * hWithDefault(x + 2)))
      else None
    }
    val coverups = groupedByX flatMap { case (k, vs) => 
      vs.map(_._2).sorted.zipWithIndex.dropWhile(x => x._1 == x._2).map(_._1 + 1) }
    math.sqrt( (heights.values ++ coverups ++ crevasses) map { x => x * x } sum)
  }
```

これでテストが通る:

```
[info] Penalty function should
[info] + penalize having blocks stacked up high
[info] + penalize having blocks covering other blocks
[info] + penalize having blocks creating deep crevasses
```

スクリプトテストをもう一回実行してみよう。消されたラインの結果だけの簡易的なアウトプットだけを書く:

```
lines: Vector(9, 11, 8, 17, 12)
```

これは 11 +/- 6 ラインだから、7 +/- 2 ラインよりも向上したと言える。

実験してみたいパラメータの一つに現在 1:10 の報酬とペナルティの比率がある。

```scala
  def utility(state: GameState): Double =
    if (state.status == GameOver) minUtility
    else reward(state) - penalty(state) / 10.0
```

ペナルティばかり書いているので、ラインを消すインセンティブが減っているのじゃないかと思っている。1:100 に変更してみよう。

```
lines: Vector(9, 11, 8, 17, 12)
```

全く同じ結果となった。スクリプトテストが無ければ予想できなかったことだ。

では、クレバスに対するペナルティに関してはどうだろう? 4 * 高さ * 高さは厳しすぎるだろうか? 高さ * 高さを試そう:

```
lines: Vector(6, 8, 8, 14, 12)
```

これは 8 +/- 6 ラインだ。4 * 高さ * 高さの 11 +/- 6 ラインに比べると全般的な劣化と言える。この定数の平方根を `crevasseWeight` と定義する:

```
c1 = lines: Vector(6, 8, 8, 14, 12)  // 8 +/- 6
c2 = lines: Vector(9, 11, 8, 17, 12) // 11 +/- 6
c3 = lines: Vector(9, 16, 8, 15, 12) // 12 +/- 4
c4 = lines: Vector(9, 16, 8, 15, 12) // 12 +/- 4
```

3 と 4 が同じ結果となったので、3 以上に増加させる意味はないだろう。
