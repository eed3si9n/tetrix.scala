---
out: refactoring.html
---

  [amazon]: http://www.amazon.co.jp/dp/4798125415

## リファクタリング

レッド、グリーン、リファクター。コピペした `rotateBy` を直そう。`Piece => Piece` の関数を受け取れば二つのメソッドの共通部分を抽出することができる。[Scala 逆引きレシピ][amazon]だと、「053: 関数を定義したい」、「054: 関数を引数として渡したい」が参考になる:

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

これで一発で `moveBy` と `rotateBy` を無くすことができた! テストを再び実行して何も壊れなかったかを確認する。

```
[info] Passed: : Total 4, Failed 0, Errors 0, Passed 4, Skipped 0
```

### 関数型へのリファクタリング

`Stage` クラスはだんだんいい形に仕上がってきてるが、二つの `var` があるのが気に入らない。状態はそれを保持する独自のクラスに追い出して `Stage` はステートレスにしよう。

```scala
case class GameState(blocks: Seq[Block], gridSize: (Int, Int), currentPiece: Piece) {
  def view: GameView = GameView(blocks, gridSize, currentPiece.current)
}
```

新しい状態を作るための `newState` メソッドを定義する:

```scala
  def newState(blocks: Seq[Block]): GameState = {
    val size = (10, 20)
    def dropOffPos = (size._1 / 2.0, size._2 - 3.0)
    val p = Piece(dropOffPos, TKind)
    GameState(blocks ++ p.current, size, p)
  }
```

それぞれの「動作」をオブジェクトへのメソッドの呼び出しと考える代わりに、一つの状態から別の状態への遷移だと考えることができる。`transformPiece` に一工夫して遷移関数を生成してみよう:

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

これで少し関数型な感じがするようになった。`transit` か実際に状態遷移関数を返しているかは型シグネチャが保証する。`Stage` がステートレスになったところで、これをシングルトンオブジェクトに変えることができる。

合わせてスペックも変更する:

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

可変実装の `moveLeft` は `this` を返したため連鎖 (chain) させることができた。新しい実装ではどうやって `leftWall1` を処理すればいいだろう? メソッドの代わりに純粋関数がある。これらは `Function.chain` を使って合成できる:

```scala
  def leftWall1 =
    Function.chain(moveLeft :: moveLeft :: moveLeft :: moveLeft :: moveLeft :: Nil)(s1).
      blocks map {_.pos} must contain(exactly(
      (0, 0), (0, 17), (1, 17), (2, 17), (1, 18)
    )).inOrder
```

`Function.chain` は `Seq[A => A]` を受け取って `A => A` の関数に変える。僕達は、この小さい部分だけだけど、コードの一部をデータ扱いしていると考えることができる。
