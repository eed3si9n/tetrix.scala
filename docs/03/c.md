---
out: drop.html
---

### drop

To speed up the game, the user should be able to drop the current piece until it hits something.

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

One way to implement this is to call `transit {_.moveBy(0.0, -1.0)}` 20 times, and then call `tick` at the end. The extra `transit` calls after hitting something would just be ignored.

```scala
  val drop: GameState => GameState = (s0: GameState) =>
    Function.chain((Nil padTo (s0.gridSize._2, transit {_.moveBy(0.0, -1.0)})) ++
      List(tick))(s0)
```

This passes the test:

```
[info]   Dropping the current piece should
[info]     + tick the piece until it hits something.
```

### summary

The current piece now moves, rotates, and drops. The full rows are cleared, and the next pieces are visible. I say the goal of finishing up the basic feature is met.

![day3](http://eed3si9n.com/images/tetrix-in-scala-day3.png)

As always, the code's up on github:

```
\$ git fetch origin
\$ git co day3v2 -b try/day3
\$ sbt swing/run
```
