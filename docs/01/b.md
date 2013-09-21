---
out: specs2.html
---

### specs2

Before we go any further we better have some specs. Testing UI-based games are not easy, but we've defined inputs and outputs in terms of data structure, so it's not that hard.

Add the latest specs2 to `library` project:

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

Here's the specs for moving the current piece:

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

Now that we have a spec, let's try some "test first" coding. Given that the initial coordinate for the piece is `(5, 17)`, it takes four `moveLeft`s to hit the wall. The subsequent `moveLeft` should be ignored.

Here's the spec for hitting the left wall:

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

As expected, this test fails:

```
[info]   Moving to the left the current piece should
[info]     + change the blocks in the view
[info]     x as long as it doesn't hit the wall.
[error]  List((0,0), (-1,17), (0,17), (1,17), (0,18)) does not contain (2,17), (1,18) and must not contain '(-1,17)' is equal to '(0,17)', '(0,18)' is equal to '(2,17)' in order (StageSpec.scala:22)
```

We'll get back to this tomorrow.

```
\$ git fetch origin
\$ git co day1v2 -b try/day1
\$ sbt swing/run
```
