---
out: other-parameters.html
---

### 他のパラメータ

高さなどの他のペナルティとのバランスを変えたらどうなるかということが気になる結果となった。

```scala
    val heightWeight = 2
    val weightedHeights = heights.values map {heightWeight * _}
```

これが結果だ:

```
h1:c3 = lines: Vector(9, 16, 8, 15, 12)   // 12 +/- 4
h2:c3 = lines: Vector(13, 19, 9, 16, 12)  // 13 +/- 6
h3:c3 = lines: Vector(20, 20, 20, 18, 43) // 20 +/- 23
h4:c3 = lines: Vector(26, 39, 11, 22, 35) // 26 +/- 13
h5:c3 = lines: Vector(22, 25, 11, 19, 16) // 19 +/- 8
```

20 +/- 23 ラインと 26 +/- 13 ラインという結果が出てきた! h4 は最小値と最大値が両方とも劣化したけど、中間値は増加した。僕の好みは最小値が大きい h3 だ。

まだテストしていないパラメータは `coverupsWeight` だ。これは `v1`、`v2`、と表記する:

```
h3:c3:v1 = lines: Vector(20, 20, 20, 18, 43) // 20 +/- 23
h3:c3:v2 = lines: Vector(11, 13, 12, 14, 17) // 13 +/- 4
```

13 +/- 4 ラインに劣化したので良いアイディアとは言えない。このペナルティごと無くしてしまったらどうだろう?

```
h3:c3:v0 = lines: Vector(35, 34, 22, 27, 33) // 33 +/- 11
```

データは嘘をつかない。33 +/- 11 ラインだ。虫歯解析は無駄だったということだ。以下が `heightWeight` と `crevasseWeight` のバランスを調整した結果だ:

```
h0:c1:v0   = lines: Vector(0, 0, 0, 1, 0)      // 0 +/- 1
h1:c2:v0   = lines: Vector(35, 21, 19, 27, 21) // 21 +/- 14
h1:c1:v0   = lines: Vector(35, 34, 22, 27, 33) // 33 +/- 11
h12:c11:v0 = lines: Vector(32, 36, 23, 46, 29) // 32 +/- 14
h11:c10:v0 = lines: Vector(34, 34, 23, 52, 29) // 34 +/- 18
h10:c9:v0  = lines: Vector(31, 34, 23, 50, 29) // 31 +/- 19
h9:c8:v0   = lines: Vector(31, 34, 24, 50, 29) // 31 +/- 19
h8:c7:v0   = lines: Vector(31, 34, 24, 50, 29) // 31 +/- 19
h7:c6:v0   = lines: Vector(31, 26, 25, 50, 29) // 29 +/- 21
h6:c5:v0   = lines: Vector(31, 26, 25, 50, 29) // 29 +/- 21
h5:c4:v0   = lines: Vector(31, 25, 14, 49, 32) // 32 +/- 18
h4:c3:v0   = lines: Vector(31, 37, 13, 44, 27) // 31 +/- 18
h3:c2:v0   = lines: Vector(40, 36, 13, 31, 20) // 31 +/- 18
h2:c1:v0   = lines: Vector(29, 29, 16, 24, 17) // 24 +/- 8
h1:c0:v0   = lines: Vector(8, 6, 8, 11, 8)     // 8 +/- 3
```

中間値によれば h11:c10:v0 が勝者だ。以下が変更された `penalty`:

```scala
  def penalty(s: GameState): Double = {
    val groupedByX = s.unload(s.currentPiece).blocks map {_.pos} groupBy {_._1}
    val heights = groupedByX map { case (k, v) => (k, v.map({_._2 + 1}).max) }
    val heightWeight = 11
    val weightedHeights = heights.values map {heightWeight * _}
    val hWithDefault = heights withDefault { x =>
      if (x < 0 || x > s.gridSize._1 - 1) s.gridSize._2
      else 0
    }
    val crevassesWeight = 10
    val crevasses = (-1 to s.gridSize._1 - 2) flatMap { x =>
      val down = hWithDefault(x + 1) - hWithDefault(x)
      val up = hWithDefault(x + 2) - hWithDefault(x + 1)
      if (down < -2 && up > 2) Some(math.min(crevassesWeight * hWithDefault(x), crevassesWeight * hWithDefault(x + 2)))
      else None
    }
    math.sqrt((weightedHeights ++ crevasses) map { x => x * x } sum)
  }
```

swing UI も健在で、なかなか良いゲームを見せてくれるようになった:

![day10](../files/tetrix-in-scala-day10.png)

また明日ここから続けよう。いつもどおりコードは github に置いてある。

```
\$ git fetch origin
\$ git co day10v2 -b try/day10
\$ sbt library/run
\$ sbt swing/run
```
