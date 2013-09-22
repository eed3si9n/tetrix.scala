---
out: hal-moment.html
---


### HAL 的な瞬間

スクリプトテストばっかりだったので、swing UI のゲームを最後まで見ることにした。ラインを次々と消しながらブロックを低めに押さえていて、ゲームのクオリティーが向上してきたことは初めから感じられた。だいたい 60 ラインぐらいを超えた所でいくつかのミスでブロックが 10 段ぐらいまで積まれてきたが、扱いきれないというほどでは無いと思った。ところが突然のようにピースをドロップし始めた。一つづつ次々と。

エージェントはゲームを放棄したかのようにみえた。後になって気付いたのはアクターのどこかでタイムアウトが生じたせいだろうということだった。

### 可変長思考サイクル

エージェントに定期的に思考するように命令するのではなく、好きなだけ考えさせてあげることにする。人の反射速度と比べてフェアなように取れるアクションは秒あたり 3つぐらいに制限する。

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

ゲームのペースを抑えるため、`Drop` は `Tick` に置換する:

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

あまりに長考するとエージェントに損なので 1000 ms ぐらいで止めておく:

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
