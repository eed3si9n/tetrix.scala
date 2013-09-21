---
out: tick.html
---

### tick

We have `moveLeft` and `moveRight`, but no `moveDown`. This is because downward movement needs to do more. Once it detects collision agaist the floor or another block, the current piece freezes at its place and a new piece gets dropped in.

First, the movement:

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

To get this test passed we can implement `tick` as using `moveBy`:

```scala
  val tick      = transit { _.moveBy(0.0, -1.0) }
```

Next, the new piece:

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

The `transit` method already knows the validity of the modified state. Currently it's just returning the old state using `getOrElse`. All we have to do is put some actions in there.

```scala
  private[this] def transit(trans: Piece => Piece,
      onFail: GameState => GameState = identity): GameState => GameState =
    (s: GameState) => validate(s.copy(
        blocks = unload(s.currentPiece, s.blocks),
        currentPiece = trans(s.currentPiece))) map { case x =>
      x.copy(blocks = load(x.currentPiece, x.blocks))
    } getOrElse {onFail(s)}
```

Unless `onFail` is passed in, it uses `identity` function. Here's the `tick`:

```scala
  val tick = transit(_.moveBy(0.0, -1.0), spawn)
  
  private[this] def spawn(s: GameState): GameState = {
    def dropOffPos = (s.gridSize._1 / 2.0, s.gridSize._2 - 3.0)
    val p = Piece(dropOffPos, TKind)
    s.copy(blocks = s.blocks ++ p.current,
      currentPiece = p)
  }
```

Let's see if this passes the test:

```
[info] Ticking the current piece should
[info] + change the blocks in the view,
[info] + or spawn a new piece when it hits something
```

### timer

Let's hook `tick` up to the down arrow key and a timer in the abstract UI:

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

This will move the current piece on its own. But since the swing UI doesn't know about it, so it won't get rendered. We can add another timer to repaint the `mainPanel` 10 fps to fix this issue:

```scala
    val timer = new SwingTimer(100, new AbstractAction() {
      def actionPerformed(e: java.awt.event.ActionEvent) { repaint }
    })
    timer.start
```

![day2](http://eed3si9n.com/images/tetrix-in-scala-day2.png)

### bottom line

The obvious issue here is that the bottom row is not clearing. Here's a spec that should test this:

```scala
                                                                              s2"""
    It should also clear out full rows.                                       \$tick3
                                                                              """
...

  val s3 = newState(Seq(
      (0, 0), (1, 0), (2, 0), (3, 0), (7, 0), (8, 0), (9, 0))
    map { Block(_, TKind) })
  def tick3 =
    Function.chain(Nil padTo (18, tick))(s3).
    blocks map {_.pos} must contain(exactly(
      (5, 0), (4, 17), (5, 17), (6, 17), (5, 18)
    )).inOrder
```

We'll get back to this tomorrow.

```
\$ git fetch origin
\$ git co day2v2 -b try/day2
\$ sbt swing/run
```
