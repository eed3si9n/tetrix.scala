
### akka 1.3.1

最新の安定版である Scala 2.9.2 と書き始めた時点で最新だった Akka 2.0.2 を選択した。問題は Akka 2.0.2 は簡単には Android で動かなそうなことだ。一方古いバージョンの Akka には
[gseitz/DiningAkkaDroids][2] という例となるアプリがあって、これは動作するらしい。特に手間でも無かったが、とにかく Akka 1.3.1 にダウングレードする必要があった。

以下に変更点をいくつか見ていく。`ActorSystem` の代わりに `Actor` というシングルトンを使ってアクターを作る。名前は `self.id` を用いて設定する:

```scala
  private[this] val stageActor1 = actorOf(new StageActor(
    stateActor1) {
    self.id = "stageActor1"
  }).start()
```

`Future` からの値の取得はこっちの方が簡単だ。`get` を呼ぶだけでいい:

```scala
  def views: (GameView, GameView) =
    ((stateActor1 ? GetView).mapTo[GameView].get,
    (stateActor2 ? GetView).mapTo[GameView].get)
```

パスのかわりに `id` を使ってアクターをルックアップする:

```scala
  private[this] def opponent: ActorRef =
    if (self.id == "stageActor1") Actor.registry.actorsFor("stageActor2")(0)
    else Actor.registry.actorsFor("stageActor1")(0)
```

スケジュールの代わりに自分で `GameMasterActor` に実装したが、これも特になんのことはない。

### Android の UI

Android にはウィジェットやグラッフィクなどに独自のライブラリがある。これらはドキュメントが整っており、他の UI プラットフォームと特に変わらない。`drawBoard` などを swing からいくつかの変更を加えるだけで移植することができた。

```scala
  var ui: Option[AbstractUI] = None

  override def run {
    ui = Some(new AbstractUI)
    var isRunning: Boolean = true
    while (isRunning) {
      val t0 = System.currentTimeMillis
      val (view1, view2) = ui.get.views
      synchronized {
        drawViews(view1, view2)
      }
      val t1 = System.currentTimeMillis
      if (t1 - t0 < quantum) Thread.sleep(quantum - (t1 - t0))
      else ()
    }
  }
  def drawViews(view1: GameView, view2: GameView) =
    withCanvas { g =>
      g drawRect (0, 0, canvasWidth, canvasHeight, bluishGray)
      val unit = blockSize + blockMargin
      val xOffset = canvasWidth / 2
      drawBoard(g, (0, 0), (10, 20), view1.blocks, view1.current)
      drawBoard(g, (12 * unit, 0), view1.miniGridSize, view1.next, Nil)
      drawStatus(g, (12 * unit, 0), view1)
      drawBoard(g, (xOffset, 0), (10, 20), view2.blocks, view2.current)
      drawBoard(g, (12 * unit + xOffset, 0), view2.miniGridSize, view2.next, Nil)
      drawStatus(g, (12 * unit + xOffset, 0), view2)
    }
```

`withCanvas` はキャンバスが解放されることを保証する loan パターンだ。問題は最近のケータイにはキーボードがついていないということだ。ここにジェスチャーの角度をアクションに変換する例を示す:

```scala
  def addFling(vx: Float, vy: Float) {
    val theta = math.toDegrees(math.atan2(vy, vx)).toInt match {
      case x if x < 0 => x + 360
      case x => x
    }
    theta match {
      case t if t < 45 || t >= 315  => ui map {_.right()}
      case t if t >= 45 && t < 135  => ui map {_.space()}
      case t if t >= 135 && t < 225 => ui map {_.left()}
      case t if t >= 225 && t < 315 => ui map {_.up()}
      case _ => // do nothing
    }
  }
```

エミュレータで読み込んでみる:

```
> android:start-emulator
```

ちょっと不安定だけど、エミュレータに表示された。

<img src="/images/tetrix-in-scala-day12.png"/>

マルチコアの Android で実行できるであろうと願っていたが、確認できた! 借り物の Galaxy S III でスムーズに実行された。

<img src="/images/tetrix-in-scala-day12b.png"/>

さて、Scala で書く tetrix もこれで最終回だ。コメントやリツイートありがとう。意見や至らない所があれば是非聞かせてほしい。それから、腕に自信がある人は人間を倒せるぐらい頭の良いエージェントアクターを pull request で送ってほしい!
