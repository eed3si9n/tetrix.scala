---
out: attacks.html
---

### 攻撃

今は二人のプレーヤが隣あってゲームをしているのと変りない。攻撃を導入しよう。どちらかのプレーヤが 2つ以上のラインを消した場合は、相手の次の転送サイクルの時点でグリッドの最下行にいくつかのブロックを加える。

ステージのスペックに記述しよう:

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

スタブする:

```scala
val notifyAttack: GameState => GameState = (s0: GameState) => s0
```

期待通りテストは失敗する:

```scala
[info] Attacks should
[error] x increment the pending attack count,
[error]    '0' is not equal to '1' (StageSpec.scala:35)
[error] x and change the blocks in the view on a tick.
[error]    '(0,0), (4,17), (5,17), (6,17), (5,18)' doesn't contain in order 
           '(1,0), (4,17), (5,17), (6,17), (5,18)' (StageSpec.scala:36)
```

これが最初の部分:

```scala
  val notifyAttack: GameState => GameState = (s0: GameState) =>
    s0.copy(pendingAttacks = s0.pendingAttacks + 1)
```

`attack` 関数は `clearFullRow` に似た形になる:

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

これは以下のように `tick` に組み込まれる:

```scala
  val tick = transit(_.moveBy(0.0, -1.0),
    Function.chain(clearFullRow :: attack :: spawn :: Nil) )
```

これでテストは通るようになった:

```
[info] Attacks should
[info] + increment the pending attack count,
[info] + and change the blocks in the view on spawn.
```

次に `StageActor` を使ってお互いの攻撃を通知する。最後の `tick` で何行のラインが消されたかを `lastDeleted` として報告する:

```scala
case class GameState(blocks: Seq[Block], gridSize: (Int, Int),
    currentPiece: Piece, nextPiece: Piece, kinds: Seq[PieceKind],
    status: GameStatus = ActiveStatus,
    lineCount: Int = 0, lastDeleted: Int = 0,
    pendingAttacks: Int = 0) {...}
```

新しいメッセージ型の `Attack` を加えて、`StageActor` で通知を実装しよう:

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

これで 2行以上のラインが消されると攻撃が行われるようになった。以下に例を見てみる:

![day11c](../files/tetrix-in-scala-day11c.png)

I字のバーが下の 4行を消して、3回の攻撃を行う。

![day11d](../files/tetrix-in-scala-day11d.png)

続きはまた明日。

```
\$ git fetch origin
\$ git co day11v2 -b try/day11
\$ sbt swing/run
```
