---
out: lines.html
---

### ライン

僕らのエージェントの幸せさは消したラインだと定義されているため、その数を覚えておく必要がある。これは `StageSpec` に入る:

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

`GameState` に `lineCount` を加えたもの:

```scala
case class GameState(blocks: Seq[Block], gridSize: (Int, Int),
    currentPiece: Piece, nextPiece: Piece, kinds: Seq[PieceKind],
    status: GameStatus, lineCount: Int) {
  def view: GameView = GameView(blocks, gridSize,
    currentPiece.current, (4, 4), nextPiece.current,
    status, lineCount)
}
```

期待通りテストは失敗:

```
[info] Deleting a full row should
[error] x increment the line count.
[error]    '0' is not equal to '1' (StageSpec.scala:91)
```

`Stage` クラスにおいて、埋まった行が消されているのは `tick` から呼ばれている `clearFullRow` 関数のみだ:

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

ちょっと見た目が怖いけど、実際のラインの消去が行われいるのが `s.copy(blocks = ...)` だと気づくだけでいい。その直後に `lineCount` を付けるだけだ:

```scala
s.copy(blocks = ...,
  lineCount = s.lineCount + 1)
```

これでテストは通った:

```
[info]   Deleting a full row should
[info]     + increment the line count.
```

これを効用関数に組み込む。

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

再び期待通りテストが失敗する:

```
[error] x evaluate an active state by lineCount
[error]    '0.0' is not equal to '1.0' (AgentSpec.scala:9)
```

これは簡単だ:

```scala
  def utility(state: GameState): Double =
    if (state.status == GameOver) -1000.0
    else state.lineCount.toDouble
```
