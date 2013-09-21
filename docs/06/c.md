---
out: solving-problems.html
---

### solving problems by searching

Now that our agent can find out how happy it is, it can turn an abtract issue of "not losing tetrix to a human" problem into tree searching problem. At any point in time, the agent and the scheduled timer can take one of the five actions we have been looking at:

```scala
  def receive = {
    case MoveLeft  => updateState {moveLeft}
    case MoveRight => updateState {moveRight}
    case RotateCW  => updateState {rotateCW}
    case Tick      => updateState {tick}
    case Drop      => updateState {drop}
  }
```

In other words, `bestMove` is a `GameState => StageMessage` function. What's with the tree? At the initial state `s0` (at time=0), the agent can take five actions: `MoveLeft`, `MoveRight` etc. The actions result in five states `s1`, `s2`, `s3`, `s4`, `s5` (at time=1). Each of the states then can branch into five more `s11`, `s12`, ..., `s55`. Draw this out, and we have a tree structure.

```
                                                  s0
                                                  |
        +--------------------+--------------------+-------...
        s1                   s2                   s3
        |                    |                    |
+---+---+---+---+    +---+---+---+---+    +---+---+---+---+ 
s11 s12 s13 s14 s15  s21 s22 s23 s24 s25  s31 s32 s33 s34 s35
```

The number of the nodes grows exponentially. `1 + 5 + 5^2`. For now, let's just start with one level.

Here's how we can contruct a test. Make a state named `s3`, which is one `Drop` action away from deleting a line. We tell the agent to pick a move, and it should select `Drop`. As a negative control, we also need some other state `s1`, which the agent can pick whatever action:

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

And here's a stub:

```scala
  def bestMove(state: GameState): StageMessage = MoveLeft
```

This fails the test as expected.

```
[info]   Solver should
[info]     + pick MoveLeft for s1
[info]     x pick Drop for s3
[error]  'MoveLeft' is not equal to 'Drop' (AgentSpec.scala:32)
```

We'll get back to this tomorrow.

```
\$ git fetch origin
\$ git co day6v2 -b try/day6
\$ sbt swing/run
```
