---
out: solving-problems.html
---

### 探索による問題解決

僕らのエージェントがどれだけ幸せかが分かるようになった所で、「人間だけには tetrix に負けない」という抽象的な問題を木の探索という問題に変えることができた。どの時点においても、エージェントとスケジュールされたタイマーは今まで何度も見た 5つのうち 1つのアクションを取ることができる:

```scala
  def receive = {
    case MoveLeft  => updateState {moveLeft}
    case MoveRight => updateState {moveRight}
    case RotateCW  => updateState {rotateCW}
    case Tick      => updateState {tick}
    case Drop      => updateState {drop}
  }
```

言い換えると、`bestMove` とは `GameState => StageMessage` の関数だ。これが木とどう関係あるって? 初期状態 `s0` (time=0 とする) において、エージェントは 5つのアクションを取れる: `MoveLeft`, `MoveRight`, etc。これらのアクションは 5つの状態 `s1`, `s2`, `s3`, `s4`, `s5` (time=1 とする) を生み出す。さらに、それぞれの状態はまた 5つに `s11`, `s12`, ..., `s55` と分岐する。これを絵に描くと木構造が見えてくる。

```
                                                  s0
                                                  |
        +--------------------+--------------------+-------...
        s1                   s2                   s3
        |                    |                    |
+---+---+---+---+    +---+---+---+---+    +---+---+---+---+ 
s11 s12 s13 s14 s15  s21 s22 s23 s24 s25  s31 s32 s33 s34 s35
```

ノード数は指数関数的に増える。`1 + 5 + 5^2`。まずは 1段階から始めよう。

テストは以下のように構築する。まず `s3` という名前の `Drop` アクションをするだけでラインが一つ消える状態を用意する。エージェントにアクションを選ばせると `Drop` を選択するべきだ。陰性対照として、もう一つ別の状態 `s1` を用意する。これは特にどのアクションを選んでもいい:

```scala
                                                                              s2"""
  Solver should
    pick MoveLeft for s1                                                      \$solver1
    pick Drop for s3                                                          \$solver2
                                                                              """
...
  def solver1 =
    agent.bestMove(s1) must_== MoveLeft
  def solver2 =
    agent.bestMove(s3) must_== Drop
```

以下がスタブだ:

```scala
  def bestMove(state: GameState): StageMessage = MoveLeft
```

期待通りテストは失敗する。

```
[info]   Solver should
[info]     + pick MoveLeft for s1
[info]     x pick Drop for s3
[error]  'MoveLeft' is not equal to 'Drop' (AgentSpec.scala:32)
```

続きはまた明日。

```
\$ git fetch origin
\$ git co day6v2 -b try/day6
\$ sbt swing/run
```
