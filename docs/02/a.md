---
out: rotation.html
---

### rotation

Now that the piece can move, we should try rotation. Given the hard-coded initial state of having T piece at `(5, 17)` and a block at `(0, 0)`, here's the spec:

```scala
                                                                              s2"""
  Rotating the current piece should
    change the blocks in the view.                                            \$rotate1
                                                                              """
...

  def rotate1 =
    rotateCW(s1).blocks map {_.pos} must contain(exactly(
      (0, 0), (5, 18), (5, 17), (5, 16), (6, 17)
    )).inOrder
```

This shouldn't even compile because `Stage` class doesn't have `rotateCW()` method yet.

```
[error] /Users/eed3si9n/work/tetrix.scala/library/src/test/scala/StageSpec.scala:33: value rorateCCW is not a member of com.eed3si9n.tetrix.Stage
[error]     stage.rotateCW().view.blocks map {_.pos} must contain(
[error]           ^
[error] one error found
[error] (library/test:compile) Compilation failed
```

Stub it out:

```scala
  def rotateCW() = this
```

and we're back to a failing test case.

First, we implement the rotation at the piece level:

```scala
  def rotateBy(theta: Double): Piece = {
    val c = math.cos(theta)
    val s = math.sin(theta)
    def roundToHalf(v: (Double, Double)): (Double, Double) =
      (math.round(v._1 * 2.0) * 0.5, math.round(v._2 * 2.0) * 0.5)
    copy(locals = locals map { case(x, y) => (x * c - y * s, x * s + y * c) } map roundToHalf)
  }
```

And then we copy-paste (!) the `moveBy` method and make it into `rotateBy`:

```scala
  def rotateCW() = rotateBy(-math.Pi / 2.0)
  private[this] def rotateBy(theta: Double): this.type = {
    validate(
        currentPiece.rotateBy(theta),
        unload(currentPiece, blocks)) map { case (moved, unloaded) =>
      blocks = load(moved, unloaded)
      currentPiece = moved
    }
    this
  }
```

This now passes the test:

```
[info]   Rotating the current piece should
[info]     + change the blocks in the view.
```
