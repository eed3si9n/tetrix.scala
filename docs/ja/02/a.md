---
out: rotation.html
---

### 回転

ピースが動くようになった所で、回転もやってみよう。初期位置 `(5, 17)` にある T字のピースと `(0, 0)` にあるブロックというハードコードされた初期状態を仮定すると、以下のようなスペックとなる:

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

`Stage` クラスには `rorateCW()` メソッドがまだないため、これはコンパイルさえしないはずだ。

```
[error] /Users/eed3si9n/work/tetrix.scala/library/src/test/scala/StageSpec.scala:33: value rorateCCW is not a member of com.eed3si9n.tetrix.Stage
[error]     stage.rotateCW().view.blocks map {_.pos} must contain(
[error]           ^
[error] one error found
[error] (library/test:compile) Compilation failed
```

最低限コンパイルは通るようにスタブを作る:

```scala
  def rotateCW() = this
```

これでまたテストが失敗するようになった。

まず、ピースの回転を実装する:

```scala
  def rotateBy(theta: Double): Piece = {
    val c = math.cos(theta)
    val s = math.sin(theta)
    def roundToHalf(v: (Double, Double)): (Double, Double) =
      (math.round(v._1 * 2.0) * 0.5, math.round(v._2 * 2.0) * 0.5)
    copy(locals = locals map { case(x, y) => (x * c - y * s, x * s + y * c) } map roundToHalf)
  }
```

次に、`moveBy` メソッドをコピペ (!) して `rotateBy` に変える:

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

テストは通過した:

```
[info]   Rotating the current piece should
[info]     + change the blocks in the view.
```
