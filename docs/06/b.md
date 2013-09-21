---
out: lines.html
---

### lines

Since my agent's happiness is defined by the lines it has deleted, we need to track that number. This goes into `StageSpec`:

```scala
                                                                              s2"""
  Deleting a full row should
    increment the line count.                                                 \$line1
                                                                              """
...
  def line1 =
    (s3.lineCount must_== 0) and
    (Function.chain(Nil padTo (19, tick))(s3).
    lineCount must_== 1)
```

Here's `GameState` with `lineCount`:

```scala
case class GameState(blocks: Seq[Block], gridSize: (Int, Int),
    currentPiece: Piece, nextPiece: Piece, kinds: Seq[PieceKind],
    status: GameStatus, lineCount: Int) {
  def view: GameView = GameView(blocks, gridSize,
    currentPiece.current, (4, 4), nextPiece.current,
    status, lineCount)
}
```

The test fails as expected:

```
[info] Deleting a full row should
[error] x increment the line count.
[error]    '0' is not equal to '1' (StageSpec.scala:91)
```

In `Stage` class, the only place full rows are deleted is in `clearFullRow` function called by `tick`:

```scala
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

It kind of looks scary, but we just have to realize that the line deletion is done using `s.copy(blocks = ...)`. We just need to add `lineCount` right afterwards:

```scala
s.copy(blocks = ...,
  lineCount = s.lineCount + 1)
```

This passes the test.

```
[info]   Deleting a full row should
[info]     + increment the line count.
```

We now need to incorporate this into the utility function.

```scala
                                                                              s2"""
    evaluate an active state by lineCount                                     \$utility3
                                                                              """
...
  def utility3 = {
    val s = Function.chain(Nil padTo (19, tick))(s3)
    agent.utility(s) must_== 1.0
  }
```

This again fails as expected:

```
[error] x evaluate an active state by lineCount
[error]    '0.0' is not equal to '1.0' (AgentSpec.scala:9)
```

This is easy:

```scala
  def utility(state: GameState): Double =
    if (state.status == GameOver) -1000.0
    else state.lineCount.toDouble
```
