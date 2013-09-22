---
out: attacks.html
---

### attacks

Currently two players are just playing side by side. Let's introduce attacks. Whenever a player deletes two or more lines in an action, some of the blocks should show up at the bottom of the opponent's grid on the next spawn cycle.

Let's spec the stage first:

```scala
                                                                              s2"""
  Attacks should
    increment the pending attack count,                                       \$attack1
    and change the blocks in the view on spawn.                               \$attack2
                                                                              """
...
  def attack1 =
    notifyAttack(s1).pendingAttacks must_== 1
  def attack2 =
    Function.chain(notifyAttack :: drop :: Nil)(s1).blocks map {_.pos} must contain(
      (0, 1), (4, 1), (5, 1), (6, 1), (5, 2),
      (4, 18), (5, 18), (6, 18), (5, 19)
    )
```

Stub it out:

```scala
val notifyAttack: GameState => GameState = (s0: GameState) => s0
```

The test fails as expected:

```scala
[info] Attacks should
[error] x increment the pending attack count,
[error]    '0' is not equal to '1' (StageSpec.scala:35)
[error] x and change the blocks in the view on a tick.
[error]    '(0,0), (4,17), (5,17), (6,17), (5,18)' doesn't contain in order
           '(1,0), (4,17), (5,17), (6,17), (5,18)' (StageSpec.scala:36)
```

Here's the first part:

```scala
  val notifyAttack: GameState => GameState = (s0: GameState) =>
    s0.copy(pendingAttacks = s0.pendingAttacks + 1)
```

The second `attack` function looks similar to `clearFullRow`:

```scala
  val attackRandom = new util.Random(0L)
  private[this] lazy val attack: GameState => GameState =
    (s0: GameState) => {
    def attackRow(s: GameState): Seq[Block] =
      (0 to s.gridSize._1 - 1).toSeq flatMap { x =>
        if (attackRandom.nextBoolean) Some(Block((x, 0), TKind))
        else None
      }
    @tailrec def tryAttack(s: GameState): GameState =
      if (s.pendingAttacks < 1) s
      else tryAttack(s.copy(
          blocks = (s.blocks map { b => b.copy(pos = (b.pos._1, b.pos._2 + 1)) } filter {
            _.pos._2 < s.gridSize._2 }) ++ attackRow(s),
          pendingAttacks = s.pendingAttacks - 1
        ))
    tryAttack(s0)
  }
```

This is added to ticking process as follows:

```scala
  val tick = transit(_.moveBy(0.0, -1.0),
    Function.chain(clearFullRow :: attack :: spawn :: Nil) )
```

All tests pass:

```
[info] Attacks should
[info] + increment the pending attack count,
[info] + and change the blocks in the view on spawn.
```

Next, we'll use `StageActor`s to notify each other's attacks. We can let the stage report the number of lines deleted in the last tick as `lastDeleted`:

```scala
case class GameState(blocks: Seq[Block], gridSize: (Int, Int),
    currentPiece: Piece, nextPiece: Piece, kinds: Seq[PieceKind],
    status: GameStatus = ActiveStatus,
    lineCount: Int = 0, lastDeleted: Int = 0,
    pendingAttacks: Int = 0) {...}
```

Add a new message type `Attack` and implement notificaton in `StageActor`:

```scala
case object Attack extends StageMessage

class StageActor(stateActor: ActorRef) extends Actor {
  import Stage._

  def receive = {
    case MoveLeft  => updateState {moveLeft}
    case MoveRight => updateState {moveRight}
    case RotateCW  => updateState {rotateCW}
    case Tick      => updateState {tick}
    case Drop      => updateState {drop}
    case Attack    => updateState {notifyAttack}
  }
  private[this] def opponent: ActorRef =
    if (self.path.name == "stageActor1") context.actorFor("/stageActor2")
    else context.actorFor("/stageActor1")
  private[this] def updateState(trans: GameState => GameState) {
    val future = (stateActor ? GetState)(1 second).mapTo[GameState]
    val s1 = Await.result(future, 1 second)
    val s2 = trans(s1)
    stateActor ! SetState(s2)
    (0 to s2.lastDeleted - 2) foreach { i =>
      opponent ! Attack
    }
  }
}
```

This creates attacks when 2 or more lines are deleted. Here's an example:

![day11c](http://eed3si9n.com/images/tetrix-in-scala-day11c.png)

The I bar is going to eliminate bottom 4 rows, creating 3 attacks.

![day11d](http://eed3si9n.com/images/tetrix-in-scala-day11d.png)

We'll pick it up from here tomorrow.

```
\$ git fetch origin
\$ git co day11v2 -b try/day11
\$ sbt swing/run
```
