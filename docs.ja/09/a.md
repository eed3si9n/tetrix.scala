---
out: second-level.html
---

### 第二段階

探索木を 2つ目のピースに拡張すると大まかに 36倍の時間がかかることになる。360ミリ秒は悪くない。やってみよう:

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

手筋が良くなってきている。思考時間は 12ミリ秒から 727ミリ秒ぐらいまで範囲があるけど、100 から 200ミリ秒ぐらいにだいたい収まっている。

![day9](http://eed3si9n.com/images/tetrix-in-scala-day9.png)

腕が上がってきたのでピースを落とさせてあげることにする:

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

### ライン数

消したライン数を swing UI に表示しよう。

```scala
    val unit = blockSize + blockMargin
    g drawString ("lines: " + view.lineCount.toString, 12 * unit, 7 * unit)
```

これでエージェントに加える変更が性能の向上につながっているかを追跡しやすくなる。

![day9b](http://eed3si9n.com/images/tetrix-in-scala-day9b.png)
