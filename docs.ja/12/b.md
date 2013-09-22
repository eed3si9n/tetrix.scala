---
out: android.html
---

  [1]: http://developer.android.com/sdk/installing/index.html
  [android-plugin]: https://github.com/jberkel/android-plugin
  [2]: https://github.com/gseitz/DiningAkkaDroids

### Android

このゲームを携帯に載せて友達に見せたら面白そうなので、Android に移植する。まず [Android SDK をインストール][1]するか、最後にいつアップデートしたか覚えてないようならばコマンドラインから `android` を起動して最新の SDK にアップデートする:

```
\$ android
```

2012年8月現在のところ最新版は Android 4.1 (API 16) だ。次に最新の API を使って Android Virtual Device (AVD) を作る。

次は sbt の [android-plugin][android-plugin] だ。`project/plugins.sbt` を作って以下を書く:

```scala
resolvers += Resolver.url("scalasbt releases", new URL("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases"))(Resolver.ivyStylePatterns)

addSbtPlugin("org.scala-sbt" % "sbt-android-plugin" % "0.6.2")
```

そして `project/build.scala` に以下を加える:

```scala
  lazy val android = Project("tetrix_android", file("android"),
    settings = buildSettings ++ Seq(
        platformName in Android := "android-8",
        versionCode := 7
      ) ++
      AndroidProject.androidSettings ++
      AndroidManifestGenerator.settings ++
      TypedResources.settings ++ Seq(

      )) dependsOn(library)
```

sbt を再読み込みすると以下のようにしてエミュレータを起動できるはずだ:

```
> project android
> android:emulator-start test_adv16
```

アプリをエミュレータにインストールして起動するには:

```
> android:start-emulator
```

ケータイにアプリをインストールするには:

```
> android:install-device
```

### hello world

Android のアプリは主にアクティビティ、ビュー、スレッドから構成される。tetrix では描画のために `Canvas` オブジェクトさえ手に入ればいいので、アクティビティもビューもかなりシンプルなものだ。ほとんどのロジックはスレッドに詰め込んでしまったが、これが正しい方法なのかは僕もよく分かっていない。

これがアクティビティのクラス:

```scala
package com.eed3si9n.tetrix.droid
  
import android.app.Activity
import android.os.Bundle
  
class MainActivity extends Activity {
  override def onCreate(savedInstanceState: Bundle ) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.main)
  }
}
```

レイアウトファイルは `android/src/main/res/layout/main.xml`:

```
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
  android:orientation="horizontal"
  android:layout_width="fill_parent"
  android:layout_height="fill_parent"
  >
    <com.eed3si9n.tetrix.droid.MainView android:id="@+id/main_view"
      android:layout_height="fill_parent"
      android:layout_width="fill_parent"
      />
</LinearLayout>
```

これは `MainView` を参照している:

```scala
package com.eed3si9n.tetrix.droid

import android.content.Context
import android.util.AttributeSet
import android.view.{View, SurfaceView, SurfaceHolder, GestureDetector, MotionEvent}

class MainView(context: Context, attrs: AttributeSet) extends SurfaceView(context, attrs) {
  val holder = getHolder
  val thread = new MainThread(holder, context)
  
  holder addCallback (new SurfaceHolder.Callback {
    def surfaceCreated(holder: SurfaceHolder) {
      thread.start()
    }
    def surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
      thread.setCanvasSize(width, height)
    }
    def surfaceDestroyed(holder: SurfaceHolder) {}
  })
  
  setFocusable(true)
  setLongClickable(true)
  setGesture()

  def setGesture() {
    val gd = new GestureDetector(new GestureDetector.SimpleOnGestureListener() {
      override def onFling(e1: MotionEvent, e2: MotionEvent,
          velocityX: Float, velocityY: Float): Boolean = {
        thread.addFling(velocityX, velocityY)
        true
      }
    })
    setOnTouchListener(new View.OnTouchListener() {
      def onTouch(v: View, e: MotionEvent): Boolean = gd.onTouchEvent(e)
    })
  }
}
```

最後に再描画のタイミングを管理するためにスレッドの中で無限ループを回す:

```scala
package com.eed3si9n.tetrix.droid

import com.eed3si9n.tetrix._
import android.content.Context
import android.view.{SurfaceHolder}
import android.graphics.{Canvas, Paint, Rect}

class MainThread(holder: SurfaceHolder, context: Context) extends Thread {
  val quantum = 100

  var canvasWidth: Int = _
  var canvasHeight: Int = _
  val bluishSilver = new Paint
  bluishSilver.setARGB(255, 210, 255, 255)
 
  override def run {
    var isRunning: Boolean = true
    while (isRunning) {
      val t0 = System.currentTimeMillis

      withCanvas { g =>
        g drawText ("hello world", 10, 10, bluishSilver)
      }
      
      val t1 = System.currentTimeMillis
      if (t1 - t0 < quantum) Thread.sleep(quantum - (t1 - t0))
      else ()
    }
  }
  def setCanvasSize(w: Int, h: Int) {
    canvasWidth = w
    canvasHeight = h
  }
  def addFling(vx: Float, vy: Float) {
    val theta = math.toDegrees(math.atan2(vy, vx)).toInt match {
      case x if x < 0 => x + 360
      case x => x
    }
    // do something
  }
  def withCanvas(f: Canvas => Unit) {
    val canvas = holder.lockCanvas(null)
    try {
      f(canvas)
    } finally {
      holder.unlockCanvasAndPost(canvas)
    }
  }
}
```

上記のコードは "hello world" を秒間10フレームで表示する。残りは組み立てだけだ。

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
