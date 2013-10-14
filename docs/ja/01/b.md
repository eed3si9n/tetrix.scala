---
out: specs2.html
---
  
  [amazon]: http://www.amazon.co.jp/dp/4798125415

### specs2

先に進む前に、そろそろスペックが必要だ。UI を使ったゲームをテストするのは容易じゃないけど、出入力をデータ構造として定義したので、それほど難しくない。[Scala 逆引きレシピ][amazon]だと、「221: Specs2でテストケースを記述したい」と「222: Specs2で実行結果を検証したい」が参考になる。

最新の spec2 を `library` プロジェクトに追加する:

```scala
lazy val specs2version = "2.2.2"
lazy val libDeps = Def.setting {
  "org.specs2" %% "specs2" % specs2version % "test"
}

lazy val library = (project in file("library")).
  settings(buildSettings: _*).
  settings(
    libraryDependencies += libDeps.value
  )
```

以下が現在のピースを移動するスペック:

```scala
import org.specs2._

class StageSpec extends Specification { def is =                              s2"""
  This is a specification to check Stage

  Moving to the left the current piece should
    change the blocks in the view.                                            \$left1

  Moving to the right the current piece should
    change the blocks in the view.                                            \$right1
                                                                              """
  
  import com.eed3si9n.tetrix._
  def stage = new Stage((10, 20))
  def left1 =
    stage.moveLeft().view.blocks map {_.pos} must contain(allOf(
      (0, 0), (3, 17), (4, 17), (5, 17), (4, 18)
    )).inOrder
  def right1 =
    stage.moveRight().view.blocks map {_.pos} must contain(allOf(
      (0, 0), (5, 17), (6, 17), (7, 17), (6, 18)
    )).inOrder
}
```

### bdd

スペックができたところで「テストファースト」のコーディングも試そう。ピースの初期座標が `(5, 17)` のとき、`moveLeft` を 4回呼ぶと壁に当たるはずだ。後続の `moveLeft` は無視するべきだ。

以下が左壁に当てるスペック:

```scala
                                                                              s2"""
  Moving to the left the current piece should
    change the blocks in the view                                             \$left1
    as long as it doesn't hit the wall.                                       \$leftWall1
                                                                              """
...

  def leftWall1 =
    stage.moveLeft().moveLeft().moveLeft().moveLeft().moveLeft().
      view.blocks map {_.pos} must contain(allOf(
      (0, 0), (0, 17), (1, 17), (2, 17), (1, 18)
    )).inOrder
```

期待通り、テストは失敗した:

```
[info]   Moving to the left the current piece should
[info]     + change the blocks in the view
[info]     x as long as it doesn't hit the wall.
[error]  List((0,0), (-1,17), (0,17), (1,17), (0,18)) does not contain (2,17), (1,18) and must not contain '(-1,17)' is equal to '(0,17)', '(0,18)' is equal to '(2,17)' in order (StageSpec.scala:22)
```

続きはまた明日。

```
\$ git fetch origin
\$ git co day1v2 -b try/day1
\$ sbt swing/run
```
