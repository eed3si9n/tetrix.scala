---
out: stream-of-pieces.html
---

### stream of pieces

It's fun, but the game is somewhat predictable because it keeps giving us Ts. The first impulse may be to generate pieces randomly. But randomness introduces side-effect, which makes it hard to test. We don't want mutability in `Stage` or `GameState`. One way to work around this is by keeping an infinite sequence of pieces in the game state. During the testing we can pass in a hard-coded `Seq[PieceKind]`.

Here are the updated `GameState` and `GameView`:

```scala
case class GameView(blocks: Seq[Block], gridSize: (Int, Int),
  current: Seq[Block], next: Seq[Block])

case class GameState(blocks: Seq[Block], gridSize: (Int, Int),
    currentPiece: Piece, nextPiece: Piece, kinds: Seq[PieceKind]) {
  def view: GameView = GameView(blocks, gridSize,
    currentPiece.current, nextPiece.current)
}
```

Here's the spec:

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

We'll pick the next piece using `s.kinds.head`, and we'll use the previously picked `nextPiece` as the `currentPiece`.

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

Running the test reveals another problem:

```
> test
[info] Compiling 1 Scala source to /Users/eed3si9n/work/tetrix.scala/library/target/scala-2.9.2/classes...
[error] Could not create an instance of StageSpec
[error]   caused by scala.MatchError: OKind (of class com.eed3si9n.tetrix.OKind\$)
[error]   com.eed3si9n.tetrix.Piece\$.apply(pieces.scala:38)
...
```

A `Piece` can't be initialized for `OKind` because we only implemented match for `TKind`. We just have to provide more local coordinates:

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

After fixing the specs by passing in a list of `TKind`s to states, all the tests pass. Here's the random stream for swing UI:

```scala
  private[this] def randomStream(random: util.Random): Stream[PieceKind] =
    PieceKind(random.nextInt % 7) #:: randomStream(random)
```

### next piece

We can now work on exposing the next piece to the UI via view.

```scala
  def onPaint(g: Graphics2D) {
    val view = ui.view
    drawBoard(g, (0, 0), view.gridSize, view.blocks, view.current)
    drawBoard(g, (12 * (blockSize + blockMargin), 0),
      view.miniGridSize, view.next, Nil) 
  }
```

`drawBoard` is extracted version of what was originally in `onPaint`.
