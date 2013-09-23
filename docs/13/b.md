---
out: think-once.html
---

### think once, move twice

Thus far the agent has been calculating a sequence of actions, and then threw everything out but the first move, to calculate the best move. In the next thinking cycle it does the same thing again, which seemed ok on a normal machine. In the phone environment the calculation is taking 10x more time. So the waste is costly in terms of game performance, since the human player and the gravity will keep moving.

We can modify the agent actor to calculate the best sequence of moves, and actually execute them instead.

```scala
case class BestMoves(s: GameState, minActionTime: Long, maxThinkTime: Long) extends AgentMessage

class AgentActor(stageActor: ActorRef) extends Actor {
  private[this] val agent = new Agent

  def receive = {
    case BestMoves(s, minActionTime, maxThinkTime) =>
      agent.bestMoves(s, maxThinkTime) match {
        case Seq(Tick) => // do nothing
        case Seq(Drop) => stageActor ! Tick
        case ms        =>
          ms foreach { _ match {
            case Tick | Drop => // do nothing
            case m           =>
              stageActor ! m
              Thread.sleep(minActionTime)
          }}
      }
      sender ! ()
  }
}
```

This worked. Now the agent can sustain the game for a while.

### config

Next I refactored configuration values that are driving this logic.

```scala
case class Config(
  minActionTime: Long,
  maxThinkTime: Long,
  onDrop: Option[StageMessage])

sealed trait AgentMessage
case class BestMoves(s: GameState, config: Config) extends AgentMessage

class AgentActor(stageActor: ActorRef) extends Actor {
  private[this] val agent = new Agent

  def receive = {
    case BestMoves(s, config) =>
      agent.bestMoves(s, config.maxThinkTime) match {
        case Seq(Tick) => // do nothing
        case Seq(Drop) => config.onDrop map { stageActor ! _ }
        case ms        =>
          ms foreach { _ match {
            case Tick | Drop => // do nothing
            case m           =>
              stageActor ! m
              Thread.sleep(config.minActionTime)
          }}
      }
      sender ! ()
  }
}
```

This way I can tweak the game blance depending on the platform. With the following setting, swing UI is quick:

```scala
  val config = Config(minActionTime = 151,
    maxThinkTime = 1500,
    onDrop = Some(Tick))
  val ui = new AbstractUI(config)
```

For Android I'm using different settings to compensate for the slower CPU:

```scala
    val config = Config(
      minActionTime = 51,
      maxThinkTime = 1000,
      onDrop = None)
    ui = Some(new AbstractUI(config))
```
