---
out: hello-world.html
---

### hello world

Android のアプリは主にアクティビティ、ビュー、スレッドから構成される。tetrix では描画のために `Canvas` オブジェクトさえ手に入ればいいので、アクティビティもビューもかなりシンプルなものだ。ほとんどのロジックはスレッドに詰め込んでしまったが、これが正しい方法なのかは僕もよく分かっていない。

まずは `android/src/main/AndroidManifest.xml` を作る。これは画面を横持ちに強制する:

```xml
<manifest package="com.eed3si9n.tetrix.droid" xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-sdk android:minSdkVersion="16"></uses-sdk>
    <application android:label="@string/app_name">
      <activity 
          android:label="@string/app_name"
          android:name=".MainActivity"
          android:launchMode="singleTop"
          android:configChanges="orientation|keyboardHidden"
          android:screenOrientation="landscape">
        <intent-filter>
          <action android:name="android.intent.action.MAIN"></action>
          <category android:name="android.intent.category.LAUNCHER"></category>
        </intent-filter>
      </activity>
    </application>
</manifest>
```

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

レイアウトファイルは `droid/src/main/res/layout/main.xml`:

```xml
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
