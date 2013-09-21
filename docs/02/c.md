---
out: collision-detection.html
---

### collision detection

For 3D games, you could write a whole book on real-time collision detection. Using Scala collection, we can write one for Tetrix in one line. Let's describe the scenario in specs:

```scala
  val s2 = newState(Block((3, 17), TKind) :: Nil)
  def leftHit1 =
    moveLeft(s2).blocks map {_.pos} must contain(
      (3, 17), (4, 17), (5, 17), (6, 17), (5, 18)
    ).only.inOrder
```

This fails as expected:

```
[error] x or another block in the grid.
[error]    '(3,17), (3,17), (4,17), (5,17), (4,18)' doesn't contain in order '(3,17), (4,17), (5,17), (6,17), (5,18)' (StageSpec.scala:9)
```

Here's the updated `validate` method:

```scala
  private[this] def validate(s: GameState): Option[GameState] = {
    val size = s.gridSize
    def inBounds(pos: (Int, Int)): Boolean =
      (pos._1 >= 0) && (pos._1 < size._1) && (pos._2 >= 0) && (pos._2 < size._2)
    val currentPoss = s.currentPiece.current map {_.pos}
    if ((currentPoss forall inBounds) && 
      (s.blocks map {_.pos} intersect currentPoss).isEmpty) Some(s)
    else None
  }
```
