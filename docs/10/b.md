---
out: crevasse.html
---

### crevasse

Any gaps with one width really creates problems. For crevasses of depth 2, only `J` or `L`, or `I` can be used to rescue it. For crevasses of depth 3 and 4, `I` would work. 

    ----------

     x
     x 
     x
     x
    xx
    ----------

The current penalty for the above figure is:

```scala
scala> val fig = math.sqrt(List(1, 5) map { x => x * x } sum)
fig: Double = 5.0990195135927845
```

Any crevasse deeper than 2 should be penalized using 4 * height * height:

```scala
scala> val fig = math.sqrt(List(1, 5, 10) map { x => x * x } sum)
fig: Double = 11.224972160321824
```

Spec this out:

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

The test fails as expected:

```
[info] Penalty function should
[info] + penalize having blocks stacked up high
[info] + penalize having blocks covering other blocks
[error] x penalize having blocks creating deep crevasses
[error]    5.0990195135927845 is not close to 11.22 +/- 0.01 (AgentSpec.scala:14)
```

To the REPL:

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

Here's the modified penalty:

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

This passes the tests:

```
[info] Penalty function should
[info] + penalize having blocks stacked up high
[info] + penalize having blocks covering other blocks
[info] + penalize having blocks creating deep crevasses
```

Let's run the scripter tests again. I am going to use more concise output:

```
lines: Vector(9, 11, 8, 17, 12)
```

It is now 11 +/- 6 lines, so that's better than 7 +/- 2 lines.

A parameter I want to try is the balance of penalty and reward, which is 1:10 now:

```scala
  def utility(state: GameState): Double =
    if (state.status == GameOver) minUtility
    else reward(state) - penalty(state) / 10.0
```

Since we have been putting more and more weights into the penalty, I wonder if the incentive to delete lines have been diminishing. Let's make it 1:100 and see what happens.

```
lines: Vector(9, 11, 8, 17, 12)
```

The result is identical. I wouldn't have guessed this without having the scripter.

How about the penalty of the crevasses? Is 4 * height * height too harsh? Let's try height * height instead.

```
lines: Vector(6, 8, 8, 14, 12)
```

This is 8 +/- 6 lines. Overall degradation compared to 11 +/- 6 lines of 4 * height * height. Let's define the square root of the constant to be `crevasseWeight`:

```
c1 = lines: Vector(6, 8, 8, 14, 12)  // 8 +/- 6
c2 = lines: Vector(9, 11, 8, 17, 12) // 11 +/- 6
c3 = lines: Vector(9, 16, 8, 15, 12) // 12 +/- 4
c4 = lines: Vector(9, 16, 8, 15, 12) // 12 +/- 4
```

3 and 4 came out to be the same, so likely there's no point in increasing beyond 3.
