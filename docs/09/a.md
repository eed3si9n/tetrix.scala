---
out: second-level.html
---

### second level

If we expanded the search tree to the second piece, it should roughly increase 36x fold. 360 ms is not bad. Let's try this:

```scala
  def bestMove(s0: GameState): StageMessage = {
    var retval: Seq[StageMessage] = Nil 
    var current: Double = minUtility
    stopWatch("bestMove") {
      val nodes = actionSeqs(s0) map { seq =>
        val ms = seq ++ Seq(Drop)
        val s1 = Function.chain(ms map {toTrans})(s0)
        val u = utility(s1)
        if (u > current) {
          current = u
          retval = seq
        } // if
        SearchNode(s1, ms, u)
      }
      nodes foreach { node =>
        actionSeqs(node.state) foreach { seq =>
          val ms = seq ++ Seq(Drop)
          val s2 = Function.chain(ms map {toTrans})(node.state)
          val u = utility(s2)
          if (u > current) {
            current = u
            retval = node.actions ++ seq
          } // if
        }
      }
    } // stopWatch
    println("selected " + retval + " " + current.toString)
    retval.headOption getOrElse {Tick}
  }
```

Now it is starting to play much better. The thinking time ranged from 12 ms to 727 ms, but mostly they were around 100 or 200 ms.

![day9](http://eed3si9n.com/images/tetrix-in-scala-day9.png)

I am now comfortable enough to letting it drop the pieces again:

```scala
class AgentActor(stageActor: ActorRef) extends Actor {
  private[this] val agent = new Agent

  def receive = {
    case BestMove(s: GameState) =>
      val message = agent.bestMove(s)
      stageActor ! message
  }
}
```

### line number

Let's display the number of deleted lines on the swing UI.

```scala
    val unit = blockSize + blockMargin
    g drawString ("lines: " + view.lineCount.toString, 12 * unit, 7 * unit)
```

Having this should help us track if our changes are improving the performance of the agent or not.

![day9b](http://eed3si9n.com/images/tetrix-in-scala-day9b.png)
