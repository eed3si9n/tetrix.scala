---
out: drop.html
---

### 落下

ゲームを早めるのに現在のピースを他の何かに当たるまで落とせる機能がほしい。

```scala
                                                                              s2"""
  Dropping the current piece should
    tick the piece until it hits something.                                   \$drop1
                                                                              """
...

  def drop1 =
    drop(s1).blocks map {_.pos} must contain(exactly(
      (0, 0), (4, 0), (5, 0), (6, 0), (5, 1),
      (4, 18), (5, 18), (6, 18), (5, 19)
    )).inOrder
```

これを実装する手軽な方法に `transit {_.moveBy(0.0, -1.0)}` を 20回呼び出して最後に `tick` を呼ぶというものがある。余分な `transit` の呼び出しは当たり判定後は無視される。

```scala
  val drop: GameState => GameState = (s0: GameState) =>
    Function.chain((Nil padTo (s0.gridSize._2, transit {_.moveBy(0.0, -1.0)})) ++
      List(tick))(s0)
```

テストは通過する:

```
[info]   Dropping the current piece should
[info]     + tick the piece until it hits something.
```

### まとめ

これで現在のピースを動かし、回転させ、落下できるようになった。埋まった列は消去され、次に出てくるピースも見えるようになった。基本機能を仕上げるという目標は一応達成したと思う。

![day3](../files/tetrix-in-scala-day3.png)

いつもどおり、コードは github にある:

```
\$ git fetch origin
\$ git co day3v2 -b try/day3
\$ sbt swing/run
```
