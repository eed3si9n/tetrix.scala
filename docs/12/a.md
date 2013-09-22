---
out: unfair-advantage.html
---

### unfair advantage

Thus far, the agent has been tuned to delete as many lines as it can. But yesterday we introduced attacking, which only kicks in after deleting two or more rows. This gives human side, who are aware of this change, an unfair advantage. Let's see what we can do.

Let's track line counts by the lines deleted in an action:

```scala
case class GameState(blocks: Seq[Block], gridSize: (Int, Int),
    currentPiece: Piece, nextPiece: Piece, kinds: Seq[PieceKind],
    status: GameStatus = ActiveStatus,
    lineCounts: Seq[Int] = Seq(0, 0, 0, 0, 0),
    lastDeleted: Int = 0, pendingAttacks: Int = 0) {
  def lineCount: Int =
    lineCounts.zipWithIndex map { case (n, i) => n * i } sum
  def attackCount: Int =
    lineCounts.drop(1).zipWithIndex map { case (n, i) => n * i } sum
  ...
}
```

Here's modified `clearFullRow`:

```scala
  private[this] lazy val clearFullRow: GameState => GameState =
    (s0: GameState) => {
    ....
    val s1 = tryRow(s0.gridSize._2 - 1, s0)
    if (s1.lastDeleted == 0) s1
    else s1.copy(lineCounts = s1.lineCounts updated
      (s1.lastDeleted, s1.lineCounts(s1.lastDeleted) + 1))
  }
```

Now modify the scripting test a bit to print out the attack count:

```scala
    println(file.getName + ": " +
      s.lineCount.toString + " lines; " +
      s.attackCount.toString + " attacks")
    (s.lineCount, s.attackCount)
```

Here are the results:

```
lines  : Vector(34, 34, 32, 52, 29)
attacks: Vector(4, 3, 3, 3, 1)
```

I can see how crevasse penalty can work against attacking, so let's increase the height penalty ratio from current 11:10 to 6:5.

```
h11:c10:v0 = lines  : Vector(34, 34, 32, 52, 29) // 34 +/- 18
h11:c10:v0 = attacks: Vector(4, 3, 3, 3, 1)      // 3 +/- 2
h6:c5:v0   = lines  : Vector(31, 26, 25, 50, 29) // 29 +/- 21
h6:c5:v0   = attacks: Vector(4, 1, 0, 5, 1)      // 1 +/- 4
```

The median actually dropped to 1. To apply the pressure to keep blocks as a chunk, we should bring back the cavity analysis:

```
h11:c10:v1 = lines  : Vector(27, 30, 24, 31, 34) // 30 +/- 4
h11:c10:v1 = attacks: Vector(0, 3, 2, 3, 1)      // 3 +/- 3
```

Maybe the problem with the cavity analysis is the the penalty calculation was too harsh in comparison with the others. Instead of piling up penalties for each block covering a cavity, why not just use height * height like crevasse:

```scala
    val coverupWeight = 1
    val coverups = groupedByX flatMap { case (k, vs) =>
      if (vs.size < heights(k)) Some(coverupWeight * heights(k))
      else None
    }
```

We'll denote this as `w1`, `w2` for the amount of `coverupWeight`.

```
h1:c1:w1   = lines  : Vector(21, 14, 16, 23, 12) // 16 +/- 7
h1:c1:w1   = attacks: Vector(2, 0, 2, 2, 1)      // 2 +/- 2
h11:c10:w2 = lines  : Vector(22, 24, 28, 50, 37) // 28 +/- 22
h11:c10:w2 = attacks: Vector(1, 0, 2, 7, 1)      // 1 +/- 6
h11:c10:w1 = lines  : Vector(39, 24, 20, 51, 34) // 34 +/- 17
h11:c10:w1 = attacks: Vector(2, 1, 1, 7, 1)      // 1 +/- 6
h22:c20:w1 = lines  : Vector(39, 24, 24, 50, 34) // 34 +/- 17
h22:c20:w1 = attacks: Vector(2, 1, 3, 7, 1)      // 3 +/- 4
h11:c10:w0 = lines  : Vector(34, 34, 32, 52, 29) // 34 +/- 18
h11:c10:w0 = attacks: Vector(4, 3, 3, 3, 1)      // 3 +/- 2
h2:c1:w1   = lines  : Vector(16, 10, 6, 18, 14)  // 14 +/- 8
h2:c1:w1   = attacks: Vector(1, 0, 0, 1, 0)      // 0 +/- 1
```

Another possibility may be to further weaken the penalty by using a constant instead of height, which we will use `k1`, `k2`:

```scala
h11:c10:k2 = lines  : Vector(34, 34, 32, 38, 29) // 34 +/- 5 
h11:c10:k2 = attacks: Vector(4, 3, 3, 5, 1)      // 3 +/- 2
h11:c10:k1 = lines  : Vector(34, 34, 32, 38, 29) // 34 +/- 5
h11:c10:k1 = attacks: Vector(4, 3, 3, 5, 1)      // 3 +/- 2
h11:c10:k0 = lines  : Vector(34, 34, 32, 52, 29) // 34 +/- 18
h11:c10:k0 = attacks: Vector(4, 3, 3, 3, 1)      // 3 +/- 2
```

In terms of the attacks per lines h11:c10:k1 is looking good, so we'll use this for now.
