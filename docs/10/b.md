---
out: crevasse.html
---

### crevasse

Any gaps with one width really creates problems. For crevasses of depth 2, only `J` or `L`, or `I` can be used to rescue it. For crevasses of depth 3 and 4, `I` would work. 

```
----------

 x
 x 
 x
 x
xx
----------
```

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
    """penalize having blocks creating deep crevasses"""    ! penalty3^
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
s: com.eed3si9n.tetrix.GameState = GameState(List(Block((0,0),TKind), Block((1,0),TKind), Block((1,1),TKind), Block((1,2),TKind), Block((1,3),TKind), Block((1,4),TKind), Block((4,18),TKind), Block((5,18),TKind), Block((6,18),TKind), Block((5,19),TKind)),(10,20),Piece((5.0,18.0),TKind,List((-1.0,0.0), (0.0,0.0), (1.0,0.0), (0.0,1.0))),Piece((2.0,1.0),TKind,List((-1.0,0.0), (0.0,0.0), (1.0,0.0), (0.0,1.0))),List(),ActiveStatus,0)

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

### other parameters

This got me thinking what if I change the balance between other penalties like height?

```scala
    val heightWeight = 2
    val weightedHeights = heights.values map {heightWeight * _}
```

Here are the results:

```
h1:c3 = lines: Vector(9, 16, 8, 15, 12)   // 12 +/- 4
h2:c3 = lines: Vector(13, 19, 9, 16, 12)  // 13 +/- 6
h3:c3 = lines: Vector(20, 20, 20, 18, 43) // 20 +/- 23
h4:c3 = lines: Vector(26, 39, 11, 22, 35) // 26 +/- 13
h5:c3 = lines: Vector(22, 25, 11, 19, 16) // 19 +/- 8
```

This is 20 +/- 23 lines and 26 +/- 13! With h4 both the min and the max performer has degraded, but the median has increased. I like h3 because it has the largest minimum.

The only parameter we haven't tested now is `coverupsWeight`. We'll denote this as `v1`, `v2`, etc:

```
h3:c3:v1 = lines: Vector(20, 20, 20, 18, 43) // 20 +/- 23
h3:c3:v2 = lines: Vector(11, 13, 12, 14, 17) // 13 +/- 4
```

This is 13 +/- 4 lines, so clearly not a good idea. How about we eliminate it from penalty altogether? 

```
h3:c3:v0 = lines: Vector(35, 34, 22, 27, 33) // 33 +/- 11
```

The data does not lie. 33 +/- 11 lines. The cavitiy analysis was useless. Here are the results from tweaking the balance of `heightWeight` and `crevasseWeight`:

```
h0:c1:v0   = lines: Vector(0, 0, 0, 1, 0)      // 0 +/- 1
h1:c2:v0   = lines: Vector(35, 21, 19, 27, 21) // 21 +/- 14
h1:c1:v0   = lines: Vector(35, 34, 22, 27, 33) // 33 +/- 11
h12:c11:v0 = lines: Vector(32, 36, 23, 46, 29) // 32 +/- 14
h11:c10:v0 = lines: Vector(34, 34, 23, 52, 29) // 34 +/- 18
h10:c9:v0  = lines: Vector(31, 34, 23, 50, 29) // 31 +/- 19
h9:c8:v0   = lines: Vector(31, 34, 24, 50, 29) // 31 +/- 19
h8:c7:v0   = lines: Vector(31, 34, 24, 50, 29) // 31 +/- 19
h7:c6:v0   = lines: Vector(31, 26, 25, 50, 29) // 29 +/- 21
h6:c5:v0   = lines: Vector(31, 26, 25, 50, 29) // 29 +/- 21
h5:c4:v0   = lines: Vector(31, 25, 14, 49, 32) // 32 +/- 18
h4:c3:v0   = lines: Vector(31, 37, 13, 44, 27) // 31 +/- 18
h3:c2:v0   = lines: Vector(40, 36, 13, 31, 20) // 31 +/- 18
h2:c1:v0   = lines: Vector(29, 29, 16, 24, 17) // 24 +/- 8
h1:c0:v0   = lines: Vector(8, 6, 8, 11, 8)     // 8 +/- 3
```

If we choose using median, h11:c10:v0 is the winner. Here's the modified `penalty`:

```scala
  def penalty(s: GameState): Double = {
    val groupedByX = s.unload(s.currentPiece).blocks map {_.pos} groupBy {_._1}
    val heights = groupedByX map { case (k, v) => (k, v.map({_._2 + 1}).max) }
    val heightWeight = 11
    val weightedHeights = heights.values map {heightWeight * _}
    val hWithDefault = heights withDefault { x =>
      if (x < 0 || x > s.gridSize._1 - 1) s.gridSize._2
      else 0
    }
    val crevassesWeight = 10
    val crevasses = (-1 to s.gridSize._1 - 2) flatMap { x =>
      val down = hWithDefault(x + 1) - hWithDefault(x)
      val up = hWithDefault(x + 2) - hWithDefault(x + 1)
      if (down < -2 && up > 2) Some(math.min(crevassesWeight * hWithDefault(x), crevassesWeight * hWithDefault(x + 2)))
      else None
    }
    math.sqrt((weightedHeights ++ crevasses) map { x => x * x } sum)
  }
```

The good old swing UI still works, and it plays nicely:

<img src="/images/tetrix-in-scala-day10.png"/>

We'll continue from here tomorrow. As always, the code is up on github.

```
\$ git fetch origin
\$ git co day10 -b try/day10
\$ sbt "project library" run
\$ sbt "project swing" run
```
