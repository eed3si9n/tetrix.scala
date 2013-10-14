---
out: grid-size.html
---

### グリッドのサイズ

何度かプレイしてみると、新しいピースの転送ポイントが低いため実質のサイズが 10x20 よりもずっと小さくなっていることに気付いた。これを回避するにはグリッドのサイズを上に伸ばして、下の 20行だけ swing UI で表示するようにすればいいと思う。数字を変えたくないのでスペックでは 10x20 のままとする。`newState` が `gridSize` を受け取るようにする:

```scala
  def newState(blocks: Seq[Block], gridSize: (Int, Int),
      kinds: Seq[PieceKind]): GameState = ...
```

あとの変更は swing UI だ。表示用に短くした `gridSize` を渡す:

```scala
    drawBoard(g, (0, 0), (10, 20), view.blocks, view.current)
    drawBoard(g, (12 * (blockSize + blockMargin), 0),
      view.miniGridSize, view.next, Nil)
```

次に範囲内のブロックだけに filter をかける:

```scala
    def drawBlocks {
      g setColor bluishEvenLigher
      blocks filter {_.pos._2 < gridSize._2} foreach { b =>
        g fill buildRect(b.pos) }
    }
    def drawCurrent {
      g setColor bluishSilver
      current filter {_.pos._2 < gridSize._2} foreach { b =>
        g fill buildRect(b.pos) }
    }
```

これで新しく転送されたピースはグリッドの上端から忍び寄ってくる。

![day5](../files/tetrix-in-scala-day5.png)

いつもどおり、コードは github にある:

```
\$ git fetch origin
\$ git co day5v2 -b try/day5
\$ sbt swing/run
```
