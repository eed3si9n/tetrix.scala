---
out: clearing-rows.html
---

### clearing rows

To clear all full rows, let's first figure out if a given row is full.

```scala
scala> s.blocks filter {_.pos._2 == 0}
res1: Seq[com.eed3si9n.tetrix.Block] = List(Block((0,0),TKind), Block((1,0),TKind), Block((2,0),TKind), Block((3,0),TKind), Block((7,0),TKind), Block((8,0),TKind), Block((9,0),TKind), Block((4,0),TKind), Block((5,0),TKind), Block((6,0),TKind))
```

We can `filter` to just row 0. We can count the size of the returned sequence to see if it's full.

```scala
scala> def isFullRow(i: Int, s: GameState): Boolean =
     | (s.blocks filter {_.pos._2 == 0} size) == s.gridSize._1
isFullRow: (i: Int, s: com.eed3si9n.tetrix.GameState)Boolean

scala> isFullRow(0, s)
res2: Boolean = true
```

Next let's figure out how to clear out the row. We can first split the `s.blocks` into parts above and below the current row.

```scala
scala> s.blocks filter {_.pos._2 < 0}
res3: Seq[com.eed3si9n.tetrix.Block] = List()

scala> s.blocks filter {_.pos._2 > 0}
res4: Seq[com.eed3si9n.tetrix.Block] = List(Block((5,1),TKind))
```

Next, we need to shift all the blocks down for ones above the cleared row.

```scala
scala> s.blocks filter {_.pos._2 > 0} map { b =>
     | b.copy(pos = (b.pos._1, b.pos._2 - 1)) }
res5: Seq[com.eed3si9n.tetrix.Block] = List(Block((5,0),TKind))
```

Here's an implementation of `clearFullRow`:

```scala
  import scala.annotation.tailrec

  private[this] lazy val clearFullRow: GameState => GameState =
    (s0: GameState) => {
    def isFullRow(i: Int, s: GameState): Boolean =
      (s.blocks filter {_.pos._2 == i} size) == s.gridSize._1
    @tailrec def tryRow(i: Int, s: GameState): GameState =
      if (i < 0) s 
      else if (isFullRow(i, s))
        tryRow(i - 1, s.copy(blocks = (s.blocks filter {_.pos._2 < i}) ++
          (s.blocks filter {_.pos._2 > i} map { b =>
            b.copy(pos = (b.pos._1, b.pos._2 - 1)) })))  
      else tryRow(i - 1, s)
    tryRow(s0.gridSize._2 - 1, s0)
  }
```

It puts together what we experimented in the REPL and wraps it in a tail recursive function. Here's the updated `tick` function to incorporate this:

```scala
  val tick = transit(_.moveBy(0.0, -1.0),
    Function.chain(clearFullRow :: spawn :: Nil) )
```

We can now run the tests to check:

```
[info]   Ticking the current piece should
[info]     + change the blocks in the view,
[info]     + or spawn a new piece when it hits something.
[info]     + It should also clear out full rows.
```

Now that the rows clear, we can take some break by playing the game.
