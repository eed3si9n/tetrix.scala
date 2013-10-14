---
out: tick.html
---

### 時計

`moveLeft` と `moveRight` があるが、`moveDown` が無い。これは下向きの動きが他にもすることがあるからだ。床か別のブロックに当たり判定が出た場合は、現在のピースが固まって、新しいピースが送り込まれる。

まずは、動きから:

```scala
                                                                              s2"""
  Ticking the current piece should
    change the blocks in the view,                                            \$tick1
                                                                              """
...

  def tick1 =
    tick(s1).blocks map {_.pos} must contain(exactly(
      (0, 0), (4, 16), (5, 16), (6, 16), (5, 17)
    )).inOrder
```

取り敢えずテストが通過するように `moveBy` を使って `tick` を実装する:

```scala
  val tick      = transit { _.moveBy(0.0, -1.0) }
```

次に、新しいピースの転送:

```scala
                                                                              s2"""
    or spawn a new piece when it hits something.                              \$tick2
                                                                              """
...

  def tick2 =
    Function.chain(Nil padTo (18, tick))(s1).
    blocks map {_.pos} must contain(exactly(
      (0, 0), (4, 0), (5, 0), (6, 0), (5, 1),
      (4, 17), (5, 17), (6, 17), (5, 18)
    )).inOrder
```

`transit` メソッドは既に変更された状態の妥当性を知ってる。現在は `getOrElse` を使って古い状態を返しているだけだけど、そこで別のアクションを実行すればいい。

```scala
  private[this] def transit(trans: Piece => Piece,
      onFail: GameState => GameState = identity): GameState => GameState =
    (s: GameState) => validate(s.copy(
        blocks = unload(s.currentPiece, s.blocks),
        currentPiece = trans(s.currentPiece))) map { case x =>
      x.copy(blocks = load(x.currentPiece, x.blocks))
    } getOrElse {onFail(s)}
```

`onFail` が渡されなければ `identity` 関数が用いられる。以下が `tick` だ:

```scala
  val tick = transit(_.moveBy(0.0, -1.0), spawn)
  
  private[this] def spawn(s: GameState): GameState = {
    def dropOffPos = (s.gridSize._1 / 2.0, s.gridSize._2 - 3.0)
    val p = Piece(dropOffPos, TKind)
    s.copy(blocks = s.blocks ++ p.current,
      currentPiece = p)
  }
```

テストを通過したか確認する:

```
[info] Ticking the current piece should
[info] + change the blocks in the view,
[info] + or spawn a new piece when it hits something
```

### タイマー

抽象UI の中で `tick` を下矢印キーとタイマーに配線しよう:

```scala
  import java.{util => ju}

  private[this] val timer = new ju.Timer
  timer.scheduleAtFixedRate(new ju.TimerTask {
    def run { state = tick(state) }
  }, 0, 1000) 

  ...

  def down() {
    state = tick(state)
  }
```

これで現在のピースが勝手に動くようになったけど、swing UI はそのことを知らないので描画はされない。`mainPanel` を 10 fps で再描画するタイマーを加えてこの問題を直す:

```scala
    val timer = new SwingTimer(100, new AbstractAction() {
      def actionPerformed(e: java.awt.event.ActionEvent) { repaint }
    })
    timer.start
```

![day2](../files/tetrix-in-scala-day2.png)

### 一番下の列

明らかな問題は一番下の列が消えていないことだ。以下のスペックでテストできると思う:

```scala
    """It should also clear out full rows."""               ! tick3^

...

  val s3 = newState(Seq(
      (0, 0), (1, 0), (2, 0), (3, 0), (7, 0), (8, 0), (9, 0))
    map { Block(_, TKind) })
  def tick3 =
  Function.chain(Nil padTo (18, tick))(s3).
    blocks map {_.pos} must contain(
      (5, 0), (4, 17), (5, 17), (6, 17), (5, 18)
    ).only.inOrder 
```

続きはまた明日。

```
\$ git fetch origin
\$ git co day2v2 -b try/day2
\$ sbt swing/run
```
