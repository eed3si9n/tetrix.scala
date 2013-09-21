---
out: game-status.html
---

### game status

Let's implement a small feature too. During the spawning process collision against existing blocks are not checked. If the new piece collides, it should end the game. Here's the spec:

```scala
                                                                              s2"""
  Spawning a new piece should
    end the game it hits something.                                           $spawn1
                                                                              """
...

  def spawn1 =
    Function.chain(Nil padTo (10, drop))(s1).status must_==
    GameOver
```

Let's define `GameStatus` trait:

```scala
sealed trait GameStatus
case object ActiveStatus extends GameStatus
case object GameOver extends GameStatus
```

The test fails as expected after adding it to the `GameStatus`:

```
[info] Spawning a new piece should
[error] x end the game it hits something.
[error]    'ActiveStatus' is not equal to 'GameOver' (StageSpec.scala:29)
```

Current implementation of `spawn` is loading `nextPiece` without checking for collision:

```scala
  private[this] lazy val spawn: GameState => GameState =
    (s: GameState) => {
    def dropOffPos = (s.gridSize._1 / 2.0, s.gridSize._2 - 2.0)
    val next = Piece((2, 1), s.kinds.head)
    val p = s.nextPiece.copy(pos = dropOffPos)
    s.copy(blocks = s.blocks ++ p.current,
      currentPiece = p, nextPiece = next, kinds = s.kinds.tail)
  }
```

All we have to do is validate the piece before loading it in.

```scala
  private[this] lazy val spawn: GameState => GameState =
    (s: GameState) => {
    def dropOffPos = (s.gridSize._1 / 2.0, s.gridSize._2 - 2.0)
    val s1 = s.copy(blocks = s.blocks,
      currentPiece = s.nextPiece.copy(pos = dropOffPos),
      nextPiece = Piece((2, 1), s.kinds.head),
      kinds = s.kinds.tail)
    validate(s1) map { case x =>
      x.copy(blocks = load(x.currentPiece, x.blocks))
    } getOrElse {
      s1.copy(blocks = load(s1.currentPiece, s1.blocks), status = GameOver)
    }
  }
```

Next, reject state transition during `GameOver` status:

```scala
  private[this] def transit(trans: Piece => Piece,
      onFail: GameState => GameState = identity): GameState => GameState =
    (s: GameState) => s.status match {
      case ActiveStatus =>
        // do transition  
      case _ => s
    }
```

Let's rub it into the player.

```scala
    view.status match {
      case GameOver =>
        g setColor bluishSilver
        g drawString ("game over",
          12 * (blockSize + blockMargin), 7 * (blockSize + blockMargin))
      case _ => // do nothing
    }
```

![day4](http://eed3si9n.com/images/tetrix-in-scala-day4.png)

As always, the code's up on github:

```
\$ git fetch origin
\$ git co day4v2 -b try/day4
\$ sbt swing/run
```
