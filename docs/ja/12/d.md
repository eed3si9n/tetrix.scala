---
out: ui-for-android.html
---

### Android の UI

Android にはウィジェットやグラッフィクなどに独自のライブラリがある。これらはドキュメントが整っており、他の UI プラットフォームと特に変わらない。`drawBoard` などを swing からいくつかの変更を加えるだけで移植することができた。

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
