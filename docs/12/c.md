---
out: hello-world.html
---

### hello world

An Android apps consists mainly of activities, views, and threads. For tetrix, we just need to get a handle to the `Canvas` object to draw things, so activities and views become fairly simple. I will stuff most of the logic in a thread, which I am not sure is the right approach.

First, create `android/src/main/AndroidManifest.xml`. This forces landscape orientation:

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

Here's the activity class:

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

The layout file is `android/src/main/res/layout/main.xml`:

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

This points to `MainView`:

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

Finally, in order to manage my own repaint, I am going to run a thread in an infinite loop:

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

The above would print out "hello world" at 10 frames per second. The rest is just the matter of hooking things up.
