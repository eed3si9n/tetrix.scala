---
out: clearing-rows.html
---

### 列の消去

埋まった列を全てを消去するために、まず与えられた列のブロックが全て埋まっているかを判定する必要がある。

```scala
scala> s.blocks filter {_.pos._2 == 0}
res1: Seq[com.eed3si9n.tetrix.Block] = List(Block((0,0),TKind), Block((1,0),TKind), Block((2,0),TKind), Block((3,0),TKind), Block((7,0),TKind), Block((8,0),TKind), Block((9,0),TKind), Block((4,0),TKind), Block((5,0),TKind), Block((6,0),TKind))
```

`filter` を使って列0 だけ取り出せる。戻ってきた列のサイズを見れば全て埋まっているかが分かる。

```scala
scala> def isFullRow(i: Int, s: GameState): Boolean =
     | (s.blocks filter {_.pos._2 == 0} size) == s.gridSize._1
isFullRow: (i: Int, s: com.eed3si9n.tetrix.GameState)Boolean

scala> isFullRow(0, s)
res2: Boolean = true
```

次に、列の消去を考える。まず、`s.blocks` を現在の列の上と下に分ける。

```scala
scala> s.blocks filter {_.pos._2 < 0}
res3: Seq[com.eed3si9n.tetrix.Block] = List()

scala> s.blocks filter {_.pos._2 > 0}
res4: Seq[com.eed3si9n.tetrix.Block] = List(Block((5,1),TKind))
```

それから、消去される列の上のブロックをずらす必要がある。

```scala
scala> s.blocks filter {_.pos._2 > 0} map { b =>
     | b.copy(pos = (b.pos._1, b.pos._2 - 1)) }
res5: Seq[com.eed3si9n.tetrix.Block] = List(Block((5,0),TKind))
```

以下は `clearFullRow` の実装の一例だ:

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

REPL で実験したことをまとめて末尾再帰の関数に入れた。`tick` を更新してこれを取り込む。

```scala
  val tick = transit(_.moveBy(0.0, -1.0),
    Function.chain(clearFullRow :: spawn :: Nil) )
```

テストを走らせて確認する:

```
[info]   Ticking the current piece should
[info]     + change the blocks in the view,
[info]     + or spawn a new piece when it hits something.
[info]     + It should also clear out full rows.
```

列が消えるようになったので、ちょっと一息ついてゲームで遊んでみよう。
