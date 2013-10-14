---
out: think-once.html
---

### 一考二行

これまでエージェントはアクションの列を計算して、初手だけを次の一手として取り出してあとは捨てていた。次の思考サイクルでもこれが繰り返される。普通のマシンの速さだとこれでも大丈夫そうだった。携帯だとこの計算に約10倍の時間がかかる。それでも人間プレーヤも重力もお構いなしなので、この無駄がゲーム性能に響いてくる。

エージェントアクターを改良して最良のアクションの列を計算させて、それを逐次実行するようにできる。

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

これはうまくいった。これでエージェントがしばらくゲームを続けられるようになった。

### 設定

このロジックの設定部分を `Config` にリファクタリングする。

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

これでプラットフォームに応じてゲームバランスを調整できるようになった。swing UI は以下の設定:

```scala
  val config = Config(minActionTime = 151,
    maxThinkTime = 1500,
    onDrop = Some(Tick))
  val ui = new AbstractUI(config)
```

Android 版は遅めの CPU に合わせる形で設定を変える:

```scala
    val config = Config(
      minActionTime = 51,
      maxThinkTime = 1000,
      onDrop = None)
    ui = Some(new AbstractUI(config))
```
