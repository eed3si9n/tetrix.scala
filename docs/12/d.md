


### akka 1.3.1

I chose the latest stable Scala 2.9.2 and Akka 2.0.2, which was the latest when I started. The problem is that Akka 2.0.2 doesn't seem to work on Android easily. On the other hand, for older version of Akka there's an example application [gseitz/DiningAkkaDroids][2] that's suppose to work. It wasn't much work, but I had to basically downgrade Akka to 1.3.1.

Here are some of the changes. Instead of an `ActorSystem`, `Actor` singleton object is used to create an actor. Names are set using `self.id`:

```scala
  private[this] val stageActor1 = actorOf(new StageActor(
    stateActor1) {
    self.id = "stageActor1"
  }).start()
```

Grabbing the values from `Future` is much simpler. You just call `get`:

```scala
  def views: (GameView, GameView) =
    ((stateActor1 ? GetView).mapTo[GameView].get,
    (stateActor2 ? GetView).mapTo[GameView].get)
```

Instead of the path, you can use `id` to lookup actors:

```scala
  private[this] def opponent: ActorRef =
    if (self.id == "stageActor1") Actor.registry.actorsFor("stageActor2")(0)
    else Actor.registry.actorsFor("stageActor1")(0)
```

I had to implement scheduling myself using `GameMasterActor`, but that wasn't a big deal either.

### UI for Android

Android has its own library for widgets and graphics. They are all well-documented, and not that much different from any other UI platforms. I was able to port `drawBoard` etc from swing with only a few modification.

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

`withCanvas` is a loan pattern that ensures that the canvas gets unlocked. The only thing is that there's no keyboard on newer phones. Here's how we can translate gesture angles into actions:

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

Let's load it on the emulator:

```
> android:start-emulator
```

It was a bit shaky but it did showed up on the emulator.

<img src="/images/tetrix-in-scala-day12.png"/>

I was hoping it runs on multicore androids, and it did! It ran smoothly on a borrowed Galaxy S III.

<img src="/images/tetrix-in-scala-day12b.png"/>

Anyway, this is going to be the end of our tetrix in Scala series. Thanks for the comments and retweets. I'd like to hear what you think. Also, if you are up for the challenge, send me a pull request of a smarter agent-actor that can beat human!
