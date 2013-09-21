---
out: stream-of-pieces.html
---

### ピースのストリーム

面白いが、T字のピースしか出てこないので少し単調だ。ピースをランダムに生成すればいいと反射的に考えるかもしれない。だけど、ランダム性は副作用を導入し、テストを難しくする。`Stage` や `GameState` に可変性を持ち込むのは避けたい。これを回避できる方法としてはゲームの状態にピースの無限列を置くことがある。テストの最中はハードコードされた `GameState` and `GameView` を入れておけばいい。

以下がが更新された `GameState` と `GameView` だ:

```scala
case class GameView(blocks: Seq[Block], gridSize: (Int, Int),
  current: Seq[Block], next: Seq[Block])

case class GameState(blocks: Seq[Block], gridSize: (Int, Int),
    currentPiece: Piece, nextPiece: Piece, kinds: Seq[PieceKind]) {
  def view: GameView = GameView(blocks, gridSize,
    currentPiece.current, nextPiece.current)
}
```

以下がスペックだ:

```scala
                                                                              s2"""
  The current piece should
    be initialized to the first element in the state.                         $init1
                                                                              """
...

  val s4 = newState(Nil, OKind :: Nil)
  def init1 =
    (s4.currentPiece.kind must_== OKind) and
    (s4.blocks map {_.pos} must contain(exactly(
      (4, 18), (5, 18), (4, 17), (5, 17)
    )).inOrder)
```

次のピースを `s.kinds.head` を用いて選び、以前に選択した `nextPiece` を `currentPiece` として使う。

```scala
  private[this] lazy val spawn: GameState => GameState =
    (s: GameState) => {
    def dropOffPos = (s.gridSize._1 / 2.0, s.gridSize._2 - 3.0)
    val next = Piece((2, 1), s.kinds.head)
    val p = s.nextPiece.copy(pos = dropOffPos)
    s.copy(blocks = s.blocks ++ p.current,
      currentPiece = p, nextPiece = next, kinds = s.kinds.tail)
  }
```

テストを実行すると別の問題が明らかになる:

```
> test
[info] Compiling 1 Scala source to /Users/eed3si9n/work/tetrix.scala/library/target/scala-2.9.2/classes...
[error] Could not create an instance of StageSpec
[error]   caused by scala.MatchError: OKind (of class com.eed3si9n.tetrix.OKind\$)
[error]   com.eed3si9n.tetrix.Piece\$.apply(pieces.scala:38)
...
```

`TKind` に対するマッチしか実装しなかったため、`Piece` を `OKind` で初期化することができない。ローカル座標をもっと提供するだけでいい:

```scala
case object PieceKind {
  def apply(x: Int): PieceKind = x match {
    case 0 => IKind
    case 1 => JKind
    case 2 => LKind
    case 3 => OKind
    case 4 => SKind
    case 5 => TKind
    case _ => ZKind
  } 
}

...

case object Piece {
  def apply(pos: (Double, Double), kind: PieceKind): Piece =
    Piece(pos, kind, kind match {
      case IKind => Seq((-1.5, 0.0), (-0.5, 0.0), (0.5, 0.0), (1.5, 0.0))      
      case JKind => Seq((-1.0, 0.5), (0.0, 0.5), (1.0, 0.5), (1.0, -0.5))
      case LKind => Seq((-1.0, 0.5), (0.0, 0.5), (1.0, 0.5), (-1.0, -0.5))
      case OKind => Seq((-0.5, 0.5), (0.5, 0.5), (-0.5, -0.5), (0.5, -0.5))
      case SKind => Seq((0.0, 0.5), (1.0, 0.5), (-1.0, -0.5), (0.0, -0.5))
      case TKind => Seq((-1.0, 0.0), (0.0, 0.0), (1.0, 0.0), (0.0, 1.0))
      case ZKind => Seq((-1.0, 0.5), (0.0, 0.5), (0.0, -0.5), (1.0, -0.5))
    })
}
```

状態に `TKind` のリストを渡してスペックを直すことで全てのテストが成功するようになっった。以下が swing UI 向けのストリームとなる:

```scala
  private[this] def randomStream(random: util.Random): Stream[PieceKind] =
    PieceKind(random.nextInt % 7) #:: randomStream(random)
```

### 次のピース

ビューを使って次のピースを UI に公開できるようになった。

```scala
  def onPaint(g: Graphics2D) {
    val view = ui.view
    drawBoard(g, (0, 0), view.gridSize, view.blocks, view.current)
    drawBoard(g, (12 * (blockSize + blockMargin), 0),
      view.miniGridSize, view.next, Nil) 
  }
```

`drawBoard` は元の `onPaint` を抽出したものだ。
