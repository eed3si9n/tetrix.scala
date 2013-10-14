---
out: unfair-advantage.html
---

### 不公平な強み

これまでの所エージェントはいかに多くのラインを消せるかということを念頭において調整されてきた。しかし昨日になって突然 2行以上を消さないと有効にならない攻撃機能が導入された。この情報を知っている人間サイドに不公平な強みができたことになる。何とかできないか少し見てみる。

まず、アクションで一度に消されたライン数を管理しよう:

```scala
case class GameState(blocks: Seq[Block], gridSize: (Int, Int),
    currentPiece: Piece, nextPiece: Piece, kinds: Seq[PieceKind],
    status: GameStatus = ActiveStatus,
    lineCounts: Seq[Int] = Seq(0, 0, 0, 0, 0),
    lastDeleted: Int = 0, pendingAttacks: Int = 0) {
  def lineCount: Int =
    lineCounts.zipWithIndex map { case (n, i) => n * i } sum
  def attackCount: Int =
    lineCounts.drop(1).zipWithIndex map { case (n, i) => n * i } sum
  ...
}
```

変更された `clearFullRow` はこうなる:

```scala
  private[this] lazy val clearFullRow: GameState => GameState =
    (s0: GameState) => {
    ....
    val s1 = tryRow(s0.gridSize._2 - 1, s0)
    if (s1.lastDeleted == 0) s1
    else s1.copy(lineCounts = s1.lineCounts updated
      (s1.lastDeleted, s1.lineCounts(s1.lastDeleted) + 1))
  }
```

スクリプティングテストにも手を加えて攻撃カウントを表示するようにする:

```scala
    println(file.getName + ": " +
      s.lineCount.toString + " lines; " +
      s.attackCount.toString + " attacks")
    (s.lineCount, s.attackCount)
```

これがその結果だ:

```
lines  : Vector(34, 34, 32, 52, 29)
attacks: Vector(4, 3, 3, 3, 1)
```

クレバスによるペナルティが攻撃の妨げになっているかもしれないので、高さによるペナルティの比率を現在の 11:10 から 6:5 に引き上げよう。

```
h11:c10:v0 = lines  : Vector(34, 34, 32, 52, 29) // 34 +/- 18
h11:c10:v0 = attacks: Vector(4, 3, 3, 3, 1)      // 3 +/- 2
h6:c5:v0   = lines  : Vector(31, 26, 25, 50, 29) // 29 +/- 21
h6:c5:v0   = attacks: Vector(4, 1, 0, 5, 1)      // 1 +/- 4
```

中間値が 1 まで落ちてしまった。ブロックが塊になるようにプレッシャーをかけるには虫歯解析を再び導入するべきだ:

```
h11:c10:v1 = lines  : Vector(27, 30, 24, 31, 34) // 30 +/- 4
h11:c10:v1 = attacks: Vector(0, 3, 2, 3, 1)      // 3 +/- 3
```

虫歯解析の問題はもしかしたら他のペナルティと比較して厳しすぎることにあるんじゃないだろうか。虫歯の上にあるブロック一つ一つに対してペナルティを課すのではなく、クレバスのペナルティのように高さ * 高さを使ってみよう:

```scala
    val coverupWeight = 1
    val coverups = groupedByX flatMap { case (k, vs) =>
      if (vs.size < heights(k)) Some(coverupWeight * heights(k))
      else None
    }
```

`coverupWeight` の変動は `w1`、`w2` と表記する。

```
h1:c1:w1   = lines  : Vector(21, 14, 16, 23, 12) // 16 +/- 7
h1:c1:w1   = attacks: Vector(2, 0, 2, 2, 1)      // 2 +/- 2
h11:c10:w2 = lines  : Vector(22, 24, 28, 50, 37) // 28 +/- 22
h11:c10:w2 = attacks: Vector(1, 0, 2, 7, 1)      // 1 +/- 6
h11:c10:w1 = lines  : Vector(39, 24, 20, 51, 34) // 34 +/- 17
h11:c10:w1 = attacks: Vector(2, 1, 1, 7, 1)      // 1 +/- 6
h22:c20:w1 = lines  : Vector(39, 24, 24, 50, 34) // 34 +/- 17
h22:c20:w1 = attacks: Vector(2, 1, 3, 7, 1)      // 3 +/- 4
h11:c10:w0 = lines  : Vector(34, 34, 32, 52, 29) // 34 +/- 18
h11:c10:w0 = attacks: Vector(4, 3, 3, 3, 1)      // 3 +/- 2
h2:c1:w1   = lines  : Vector(16, 10, 6, 18, 14)  // 14 +/- 8
h2:c1:w1   = attacks: Vector(1, 0, 0, 1, 0)      // 0 +/- 1
```

もう一つの可能性としては、ペナルティを高さのかわりにに定数にしてしまうことでさらに弱めるということができる。これは `k1`、`k2` と書く:

```scala
h11:c10:k2 = lines  : Vector(34, 34, 32, 38, 29) // 34 +/- 5 
h11:c10:k2 = attacks: Vector(4, 3, 3, 5, 1)      // 3 +/- 2
h11:c10:k1 = lines  : Vector(34, 34, 32, 38, 29) // 34 +/- 5
h11:c10:k1 = attacks: Vector(4, 3, 3, 5, 1)      // 3 +/- 2
h11:c10:k0 = lines  : Vector(34, 34, 32, 52, 29) // 34 +/- 18
h11:c10:k0 = attacks: Vector(4, 3, 3, 3, 1)      // 3 +/- 2
```

ライン当たりの攻撃数では h11:c10:k1 がいい感じなので、これを採用しよう。
