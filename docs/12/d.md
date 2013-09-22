---
out: ui-for-android.html
---

### UI for Android

Android has its own library for widgets and graphics. They are all well-documented, and not that much different from any other UI platforms. I was able to port `drawBoard` etc from swing with only a few modification.

```scala
  var blockSize: Int = 18
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
      blockSize = canvasHeight / 22
      bluishSilver.setTextSize(blockSize)
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

To load it on the emulator, select the device and `android:run`:

```
tetrix_droid> devices
[info] Connected devices:
[info]   emulator-5554          test_adv16
tetrix_droid> device emulator-5554
[info] default device: emulator-5554
tetrix_droid> android:run
[info] Generating R.java
[info] [debug] cache hit, skipping proguard!
[info] classes.dex is up-to-date
[info] Debug package does not need signing: tetrix_droid-debug-unaligned.apk
[info] zipaligned: tetrix_droid-debug.apk
[info] Installing...
```

It did showed up on the emulator.

![day12c](http://eed3si9n.com/images/tetrix-in-scala-day12c.png)

To really get the feeling on how it works, let's load on a phone. Here's tetrix running on my htc one:

![day12d](http://eed3si9n.com/images/tetrix-in-scala-day12d.jpg)

Anyway, this is going to be the end of our tetrix in Scala series. Thanks for the comments and retweets. I'd like to hear what you think. Also, if you are up for the challenge, send me a pull request of a smarter agent-actor that can beat human!
