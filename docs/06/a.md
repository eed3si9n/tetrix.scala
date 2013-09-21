---
out: aima.html
---

  [russell]: http://aima.cs.berkeley.edu/

### Russell and Norvig

One of the reasons I picked CS major at my college was to learn about AI. It was quite disappointing that in the first few years none of my classes covered anything like AI. So during a summer co-op (internship) I decided to wake up early, go to Starbucks, and read a textbook smart colleges were using to teach AI. That's how I found Russell and Norvig's [Artificial Intelligence: A Modern Approach (AIMA)][russell].

The book was shocking. Instead of trying to create a human-like robot, it introduces a concept called agent, which *does* something rational.

> An **agent** is anything that can be viewed as perceiving its environment through sensors and acting upon that environment through actuators.

One of the structures of rational agent is a model-based, utility-based agent.

```
+-agent-------------------+   +-environment-+ 
|           Sensors      <=====             |
|   State <----+          |   |             |
|              |          |   |             |
| What if I do action A?  |   |             |
|              |          |   |             |
|   How happy will I be?  |   |             |
|              |          |   |             |
| Utility <----+          |   |             |
|              |          |   |             |
|  What should I do next? |   |             |
|              |          |   |             |
|           Actuators     =====>            |
+-------------------------+   +-------------+
```

> A utility function maps a state (or a sequence of states) onto a real number, which describes the associated degree of happiness.

Blows your mind, right? Using this structure, we can make a program that appears intelligent by constructing a state machine (done!), a utility function, and a tree searching algorithm. The data structure and graph theory can be useful after all.

### utility function

For a utility-based agent, construction of the utility function is the key. We will probably be tweak this going forward, but let's start with something simple. For now, I define that the happiness is not being dead, and the deleted lines. As passive as it sounds, tetrix is a game of not losing. On one-on-one tetrix, there isn't a clear definition of winning. You win by default when the opponent loses.

Let's describe this in a new spec:

```scala
import org.specs2._

class AgentSpec extends Specification with StateExample { def is =            s2"""
  This is a specification to check Agent

  Utility function should
    evaluate initial state as 0.0,                                            \$utility1
    evaluate GameOver as -1000.0,                                             \$utility2
                                                                              """
  
  import com.eed3si9n.tetrix._
  import Stage._

  val agent = new Agent

  def utility1 =
    agent.utility(s1) must_== 0.0 
  def utility2 =
    agent.utility(gameOverState) must_== -1000.0
}
```

Next we start `Agent` class and stub the `utility` method:

```scala
package com.eed3si9n.tetrix

class Agent {
  def utility(state: GameState): Double = 0.0
}
```

This fails the second example as expected:

```
[info] Utility function should
[info] + evaluate initial state as 0.0,
[error] x evaluate GameOver as -1000.0.
[error]    '0.0' is not equal to '-1000.0' (AgentSpec.scala:8)
```

Let's fix this:

```scala
  def utility(state: GameState): Double =
    if (state.status == GameOver) -1000.0
    else 0.0
```

All green. Nothing to refactor here.
