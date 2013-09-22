package com.eed3si9n.tetrix.droid

import android.content.Context
import android.util.AttributeSet
import android.view.{SurfaceView, SurfaceHolder}

class MainView(context: Context, attrs: AttributeSet) extends SurfaceView(context, attrs) {
  val holder = getHolder
  val thread = new MainThread(holder, context)
  
  holder addCallback (new SurfaceHolder.Callback {
    def surfaceCreated(holder: SurfaceHolder) {
      thread.start()     
    }
    def surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
    }
    def surfaceDestroyed(holder: SurfaceHolder) {}
  })
  
  setFocusable(true)
  setLongClickable(true)
}
