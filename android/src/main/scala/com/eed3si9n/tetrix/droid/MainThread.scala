package com.eed3si9n.tetrix.droid

import com.eed3si9n.tetrix._
import android.content.Context
import android.view.{SurfaceHolder}
import android.graphics.{Canvas, Paint}

class MainThread(holder: SurfaceHolder, context: Context) extends Thread {
  val quantum = 100

  override def run {
    val p = new Paint
    p.setARGB(255, 100, 100, 100)

    // val ui = new AbstractUI
    var isRunning: Boolean = true
    while (isRunning) {
      val t0 = System.currentTimeMillis
      // val (view1, view2) = ui.views
      // drawViews(view1, view2)
      withCanvas { canvas =>
        canvas.drawText("hello world", 10, 10, p)
      }

      val t1 = System.currentTimeMillis
      if (t1 - t0 < quantum) Thread.sleep(quantum - (t1 - t0))
      else ()
    }
  }

  // def drawViews(view1: GameView, view2: GameView) =
  //   withCanvas { canvas =>
  //     val p = new Paint
  //     p.setARGB(255, 100, 100, 100)
  //     view1.status match {
  //       case GameOver => canvas.drawText("game over", 10, 10, p)
  //       case _ => canvas.drawText("active", 10, 10, p)
  //     }
  //   }

  def withCanvas(f: Canvas => Unit) {
    val canvas = holder.lockCanvas(null)
    try {
      f(canvas)
    } finally {
      holder.unlockCanvasAndPost(canvas)
    }
  }
}
