---
out: refactoring.html
---

## refactoring

Red, green, and refactor. Let's fix the copy-pasted `rotateBy`. We can extract out common parts by simply accepting a function `Piece => Piece`:

```scala
  def moveLeft() = transformPiece(_.moveBy(-1.0, 0.0))
  def moveRight() = transformPiece(_.moveBy(1.0, 0.0))
  def rotateCW() = transformPiece(_.rotateBy(-math.Pi / 2.0))
  private[this] def transformPiece(trans: Piece => Piece): this.type = {
    validate(
        trans(currentPiece),
        unload(currentPiece, blocks)) map { case (moved, unloaded) =>
      blocks = load(moved, unloaded)
      currentPiece = moved
    }
    this
  }
```

This gets rid of the `moveBy` and `rotateBy` in a single shot! Run the tests again to make sure we didn't break anything.

```
[info] Passed: : Total 4, Failed 0, Errors 0, Passed 4, Skipped 0
```

### functional refactoring

`Stage` class is shaping up to be a nice class, but I really don't like the fact that it has two `var`s in it. Let's kick out the states into its own class so we can make `Stage` stateless.

```scala
case class GameState(blocks: Seq[Block], gridSize: (Int, Int), currentPiece: Piece) {
  def view: GameView = GameView(blocks, gridSize, currentPiece.current)
}
```

Let's define a `newState` method to start a new state:

```scala
  def newState(blocks: Seq[Block]): GameState = {
    val size = (10, 20)
    def dropOffPos = (size._1 / 2.0, size._2 - 3.0)
    val p = Piece(dropOffPos, TKind)
    GameState(blocks ++ p.current, size, p)
  }
```

We can now think of each "moves" as transition from one state to another instead of calling methods on an object. We can tweak the `transformPiece` to generate transition functions:

```scala
  val moveLeft  = transit { _.moveBy(-1.0, 0.0) }
  val moveRight = transit { _.moveBy(1.0, 0.0) }
  val rotateCW  = transit { _.rotateBy(-math.Pi / 2.0) }
  private[this] def transit(trans: Piece => Piece): GameState => GameState =
    (s: GameState) => validate(s.copy(
        blocks = unload(s.currentPiece, s.blocks),
        currentPiece = trans(s.currentPiece))) map { case x =>
      x.copy(blocks = load(x.currentPiece, x.blocks))
    } getOrElse {s}
  private[this] def validate(s: GameState): Option[GameState] = {
    val size = s.gridSize
    def inBounds(pos: (Int, Int)): Boolean =
      (pos._1 >= 0) && (pos._1 < size._1) && (pos._2 >= 0) && (pos._2 < size._2)
    if (s.currentPiece.current map {_.pos} forall inBounds) Some(s)
    else None
  }
```

This feels more functional style. The type signature makes sure that `transit` does in fact return a state transition function. Now that `Stage` is stateless, we can turn it into a singleton object.

The specs needs a few modification:

```scala
  import com.eed3si9n.tetrix._
  import Stage._
  val s1 = newState(Block((0, 0), TKind) :: Nil)
  def left1 =
    moveLeft(s1).blocks map {_.pos} must contain(exactly(
      (0, 0), (3, 17), (4, 17), (5, 17), (4, 18)
    )).inOrder
  def leftWall1 = sys.error("hmmm")
    // stage.moveLeft().moveLeft().moveLeft().moveLeft().moveLeft().
    //  view.blocks map {_.pos} must contain(exactly(
    //  (0, 0), (0, 17), (1, 17), (2, 17), (1, 18)
    // )).inOrder
  def right1 =
    moveRight(s1).blocks map {_.pos} must contain(exactly(
      (0, 0), (5, 17), (6, 17), (7, 17), (6, 18)
    )).inOrder
  def rotate1 =
    rotateCW(s1).blocks map {_.pos} must contain(excactly(
      (0, 0), (5, 18), (5, 17), (5, 16), (6, 17)
    )).inOrder
```

The mutable implementation of `moveLeft` returned `this` so we were able to chain them. How should we handle `leftWall1`? Instead of methods, we now have pure functions. These can be composed using `Function.chain`:

```scala
  def leftWall1 =
    Function.chain(moveLeft :: moveLeft :: moveLeft :: moveLeft :: moveLeft :: Nil)(s1).
      blocks map {_.pos} must contain(exactly(
      (0, 0), (0, 17), (1, 17), (2, 17), (1, 18)
    )).inOrder
```

`Function.chain` takes a `Seq[A => A]` and turns it into an `A => A` function. We are essentially treating a tiny part of the code as data.
