---
out: man-vs-machine.html
---

### 人対マシン

エージェントの調整ができたので、当然次のステップは人間相手にプレイすることだ。同一の初期状態から 2つのステージアクターを用意しよう。一つはプレーヤにより制御され、もう一つはエージェントにより制御される。

```scala
  private[this] val initialState = Stage.newState(Nil,
    (10, 23), Stage.randomStream(new util.Random))
  private[this] val system = ActorSystem("TetrixSystem")
  private[this] val stateActor1 = system.actorOf(Props(new StateActor(
    initialState)), name = "stateActor1")
  private[this] val stageActor1 = system.actorOf(Props(new StageActor(
    stateActor1)), name = "stageActor1")
  private[this] val stateActor2 = system.actorOf(Props(new StateActor(
    initialState)), name = "stateActor2")
  private[this] val stageActor2 = system.actorOf(Props(new StageActor(
    stateActor2)), name = "stageActor2")
  private[this] val agentActor = system.actorOf(Props(new AgentActor(
    stageActor2)), name = "agentActor")
  private[this] val masterActor = system.actorOf(Props(new GameMasterActor(
    stateActor2, agentActor)), name = "masterActor")
  private[this] val tickTimer1 = system.scheduler.schedule(
    0 millisecond, 701 millisecond, stageActor1, Tick)
  private[this] val tickTimer2 = system.scheduler.schedule(
    0 millisecond, 701 millisecond, stageActor2, Tick)
  
  masterActor ! Start

  def left()  { stageActor1 ! MoveLeft }
  def right() { stageActor1 ! MoveRight }
  def up()    { stageActor1 ! RotateCW }
  def down()  { stageActor1 ! Tick }
  def space() { stageActor1 ! Drop }
```

現在の `view` は 1つのビューしか返さないため、ペアを返すように変更する。

```scala
  def views: (GameView, GameView) =
    (Await.result((stateActor1 ? GetView).mapTo[GameView], timeout.duration),
    Await.result((stateActor2 ? GetView).mapTo[GameView], timeout.duration))
```

次に swing UI が両方のビューを描画できるようにする。

```scala
  def onPaint(g: Graphics2D) {
    val (view1, view2) = ui.views
    val unit = blockSize + blockMargin
    val xOffset = mainPanelSize.width / 2
    drawBoard(g, (0, 0), (10, 20), view1.blocks, view1.current)
    drawBoard(g, (12 * unit, 0), view1.miniGridSize, view1.next, Nil)
    drawStatus(g, (12 * unit, 0), view1)
    drawBoard(g, (xOffset, 0), (10, 20), view2.blocks, view2.current)
    drawBoard(g, (12 * unit + xOffset, 0), view2.miniGridSize, view2.next, Nil)
    drawStatus(g, (12 * unit + xOffset, 0), view2)
  }
  def drawStatus(g: Graphics2D, offset: (Int, Int), view: GameView) {
    val unit = blockSize + blockMargin
    g setColor bluishSilver
    view.status match {
      case GameOver =>
        g drawString ("game over", offset._1, offset._2 + 8 * unit)
      case _ => // do nothing
    }
    g drawString ("lines: " + view.lineCount.toString, offset._1, offset._2 + 7 * unit)
  }
```

既に `drawBoard` がリファクタ済みだったため、思ったより簡単だった。

![day11](http://eed3si9n.com/images/tetrix-in-scala-day11.png)

`GameMasterActor` にレフェリーになってもらってどちらかが負けた時点で勝者も決定するようにする。

```scala
case object Victory extends GameStatus
...

class GameMasterActor(stateActor1: ActorRef, stateActor2: ActorRef,
    agentActor: ActorRef) extends Actor {
  ...

  private[this] def getStatesAndJudge: (GameState, GameState) = {
    var s1 = getState1
    var s2 = getState2
    if (s1.status == GameOver && s2.status != Victory) {
      stateActor2 ! SetState(s2.copy(status = Victory))
      s2 = getState2
    }
    if (s1.status != Victory && s2.status == GameOver) {
      stateActor1 ! SetState(s1.copy(status = Victory))
      s1 = getState1
    }
    (s1, s2)
  }
}
```

ステータスを UI に表示しよう:

```scala
      case Victory =>
        g drawString ("you win!", offset._1, offset._2 + 8 * unit)
```

こんな感じになる:

![day11b](http://eed3si9n.com/images/tetrix-in-scala-day11b.png)
