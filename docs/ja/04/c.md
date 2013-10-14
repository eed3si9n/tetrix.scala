---
out: game-status.html
---

### ゲームステータス

小さくてもいいから何か機能も追加しよう。新しいピースの転送処理の時に既存のブロックに対する当たり判定が行われていない。もし新しいピースに当たりが検知された場合はゲームは終了するべきだ。以下がスペックになる:

```scala
                                                                              s2"""
  Spawning a new piece should
    end the game it hits something.                                           \$spawn1
                                                                              """
...

  def spawn1 =
    Function.chain(Nil padTo (10, drop))(s1).status must_==
    GameOver
```

コンパイルが通るように `GameStatus` トレイトから定義していく:

```scala
sealed trait GameStatus
case object ActiveStatus extends GameStatus
case object GameOver extends GameStatus
```

これを `GameStatus` に追加すると期待通りテストが失敗するようになった:

```
[info] Spawning a new piece should
[error] x end the game it hits something.
[error]    'ActiveStatus' is not equal to 'GameOver' (StageSpec.scala:29)
```

`spawn` の現行の実装は `nextPiece` を当たり判定無しで取り込んでいる:

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

ピースを取り込む前に検証に通そう。

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

次に、ステータスが `GameOver` のときは状態遷移を禁止する:

```scala
  private[this] def transit(trans: Piece => Piece,
      onFail: GameState => GameState = identity): GameState => GameState =
    (s: GameState) => s.status match {
      case ActiveStatus =>
        // do transition  
      case _ => s
    }
```

プレーヤにも一言言っておく。

```scala
    view.status match {
      case GameOver =>
        g setColor bluishSilver
        g drawString ("game over",
          12 * (blockSize + blockMargin), 7 * (blockSize + blockMargin))
      case _ => // do nothing
    }
```

![day4](../files/tetrix-in-scala-day4.png)

いつもどおり、コードは github にある:

```
\$ git fetch origin
\$ git co day4v2 -b try/day4
\$ sbt swing/run
```
