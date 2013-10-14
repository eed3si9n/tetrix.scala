---
out: collision-detection.html
---

### 当たり判定

3D ゲームだとリアルタイムでの当たり判定だけで本が一冊書ける。2D の落ちゲーの場合は Scala コレクションを使うと一行で書ける。シナリオをスペックで記述してみよう:

```scala
  val s2 = newState(Block((3, 17), TKind) :: Nil)
  def leftHit1 =
    moveLeft(s2).blocks map {_.pos} must contain(
      (3, 17), (4, 17), (5, 17), (6, 17), (5, 18)
    ).only.inOrder
```

これは期待通り失敗してくれる:

```
[error] x or another block in the grid.
[error]    '(3,17), (3,17), (4,17), (5,17), (4,18)' doesn't contain in order '(3,17), (4,17), (5,17), (6,17), (5,18)' (StageSpec.scala:9)
```

これが当たり判定を加えた `validate` メソッドだ:

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
