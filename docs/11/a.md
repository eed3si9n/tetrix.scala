---
out: hal-moment.html
---

### HAL moment

I decided to watch a game through on the swing UI, since I had been script testing mostly. From the beginning I could feel the improvement of the game quality as it was able to manage the blocks lows, and kept deleting the lines. After it went past 60 lines, it made a few mistakes and the blocks started to stack up to maybe 10th row, but nothing unmanagable. Then, all of a sudden, the agent started droping the piece. One after another.

It was as if the agent suddently gave up the game. Later during the day I realized that likely it had reached a timeout in one of the actors.

### variable thinking cycle

Instead of telling the agent to think at a regular interval, let's let it think as long as it wants to. To be fair to human response time, let's throttle an action to around 3 per second.

```scala
sealed trait GameMasterMessage
case object Start

class GameMasterActor(stateActor: ActorRef, agentActor: ActorRef) extends Actor {
  def receive = {
    case Start => loop 
  }
  private[this] def loop {
    val minActionTime = 337
    var s = getState
    while (s.status != GameOver) {
      val t0 = System.currentTimeMillis
      agentActor ! BestMove(getState)
      val t1 = System.currentTimeMillis
      if (t1 - t0 < minActionTime) Thread.sleep(minActionTime - (t1 - t0))
      s = getState
    }
  }
  private[this] def getState: GameState = {
    val future = (stateActor ? GetState)(1 second).mapTo[GameState]
    Await.result(future, 1 second)
  } 
}
```

In order to slow down the game a bit, let's substitute `Drop` for a `Tick` also:

```scala
class AgentActor(stageActor: ActorRef) extends Actor {
  private[this] val agent = new Agent

  def receive = {
    case BestMove(s: GameState) =>
      val message = agent.bestMove(s)
      if (message == Drop) stageActor ! Tick
      else stageActor ! message
  }
}
```

To prevent the agent from taking too long time to think, let's cap it to 1000 ms.

```scala
  val maxThinkTime = 1000
  val t0 = System.currentTimeMillis
  ...
  nodes foreach { node =>
    if (System.currentTimeMillis - t0 < maxThinkTime)
      actionSeqs(node.state) foreach { seq =>
        ...
      }
    else ()
  }
```
