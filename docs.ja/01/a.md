---
out: drawing-the-game.html
---

### ゲームの描画

これはゲームの描画を改善するのに十分な情報だ。

```scala
  def onPaint(g: Graphics2D) {
    val view = ui.view

    def buildRect(pos: (Int, Int)): Rectangle =
      new Rectangle(pos._1 * (blockSize + blockMargin),
        (view.gridSize._2 - pos._2 - 1) * (blockSize + blockMargin),
        blockSize, blockSize)
    def drawEmptyGrid {
      g setColor bluishLigherGray
      for {
        x <- 0 to view.gridSize._1 - 1
        y <- 0 to view.gridSize._2 - 2
        val pos = (x, y)
      } g draw buildRect(pos)      
    }
    def drawBlocks {
      g setColor bluishEvenLigher
      view.blocks foreach { b => g fill buildRect(b.pos) }
    }
    def drawCurrent {
      g setColor bluishSilver
      view.current foreach { b => g fill buildRect(b.pos) }
    }
    drawEmptyGrid
    drawBlocks
    drawCurrent
  }
```

ゲームの状態が可視化できたところで、次は振る舞いも実装してみよう。

### ステージ

現在のピースを表すのにブロックの列よりもいい方法が必要だ。`Piece` クラスは現在位置を `(Double, Double)` で保持して `current` をローカル座標系から算出する。

```scala
case class Piece(pos: (Double, Double), kind: PieceKind, locals: Seq[(Double, Double)]) {
  def current: Seq[Block] =
    locals map { case (x, y) => 
      Block((math.floor(x + pos._1).toInt, math.floor(y + pos._2).toInt), kind)
    }
}
case object Piece {
  def apply(pos: (Double, Double), kind: PieceKind): Piece =
    kind match {
      case TKind => Piece(pos, kind, Seq((-1.0, 0.0), (0.0, 0.0), (1.0, 0.0), (0.0, 1.0)))
    }
}
```

これを使ってゲームの世界の物理系を司る `Stage` を定義できる。

```scala
package com.eed3si9n.tetrix

class Stage(size: (Int, Int)) {
  private[this] def dropOffPos = (size._1 / 2.0, size._2 - 3.0)
  private[this] var currentPiece = Piece(dropOffPos, TKind)
  private[this] var blocks = Block((0, 0), TKind) +: currentPiece.current
  def view: GameView = GameView(blocks, size, currentPiece.current)
}
```

現在のピースを移動させるには、まずそれをグリッドから外に出して (unload)、新しい位置に移動し、グリッドに再読み込みする。

`Piece` クラスの `moveBy` はこうなる:

```scala
  def moveBy(delta: (Double, Double)): Piece =
    copy(pos = (pos._1 + delta._1, pos._2 + delta._2))
```

これが unload と load だ:

```scala
class Stage(size: (Int, Int)) {
  ...

  def moveLeft() = moveBy(-1.0, 0.0)
  def moveRight() = moveBy(1.0, 0.0)
  private[this] def moveBy(delta: (Double, Double)): this.type = {
    val unloaded = unload(currentPiece, blocks)
    val moved = currentPiece.moveBy(delta)
    blocks = load(moved, unloaded)
    currentPiece = moved
    this
  }
  private[this] def unload(p: Piece, bs: Seq[Block]): Seq[Block] = {
    val currentPoss = p.current map {_.pos}
    bs filterNot { currentPoss contains _.pos  }
  }
  private[this] def load(p: Piece, bs: Seq[Block]): Seq[Block] =
    bs ++ p.current
}
```

### つなげる

ステージを抽象UI につなげてみよう:

```scala
package com.eed3si9n.tetrix

class AbstractUI {
  private[this] val stage = new Stage((10, 20))
  def left() {
    stage.moveLeft()
  }
  def right() {
    stage.moveRight()
  }
  def up() {
  }
  def down() {
  }
  def space() {
  }
  def view: GameView = stage.view
}
```

これで swing UI を起動するとピースが移動するのが確認できるはずだ。

![day1](http://eed3si9n.com/images/tetrix-in-scala-day1.png)
