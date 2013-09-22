package com.eed3si9n.tetrix.droid

import com.eed3si9n.tetrix._
import android.content.Context
import android.view.{SurfaceHolder}
import android.graphics.{Canvas, Paint, Rect}

class MainThread(holder: SurfaceHolder, context: Context) extends Thread {
  val quantum = 100

  var canvasWidth: Int = _
  var canvasHeight: Int = _
  var blockSize: Int = 18
  var ui: Option[AbstractUI] = None
  val bluishGray = new Paint
  bluishGray.setARGB(255, 48, 99, 99)
  val bluishLigherGray = new Paint
  bluishLigherGray.setARGB(255, 79, 130, 130)
  bluishLigherGray.setStyle(Paint.Style.FILL)
  val bluishEvenLigher = new Paint
  bluishEvenLigher.setARGB(255, 145, 196, 196)
  val bluishSilver = new Paint
  bluishSilver.setARGB(255, 210, 255, 255)
  val blockMargin = 1  

  override def run {
    ui = Some(new AbstractUI)
    var isRunning: Boolean = true
    while (isRunning) {
      val t0 = System.currentTimeMillis
      val (view1, view2) = ui.get.views
      drawViews(view1, view2)
      val t1 = System.currentTimeMillis
      if (t1 - t0 < quantum) Thread.sleep(quantum - (t1 - t0))
      else ()
    }
  }
  def setCanvasSize(w: Int, h: Int) {
    canvasWidth = w
    canvasHeight = h
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
  def drawStatus(g: Canvas, offset: (Int, Int), view: GameView) {
    val unit = blockSize + blockMargin
    view.status match {
      case GameOver =>
        g drawText ("game over", offset._1, offset._2 + 8 * unit, bluishSilver)
      case Victory =>
        g drawText ("you win!", offset._1, offset._2 + 8 * unit, bluishSilver)
      case _ => // do nothing
    }
    g drawText ("lines: " + view.lineCount.toString, offset._1, offset._2 + 7 * unit, bluishSilver)
  }
  def drawBoard(g: Canvas, offset: (Int, Int), gridSize: (Int, Int), 
      blocks: Seq[Block], current: Seq[Block]) {
    // left, top, right, bottom
    def buildRect(pos: (Int, Int)): Rect =
      new Rect(
        offset._1 + pos._1 * (blockSize + blockMargin),
        offset._2 + (gridSize._2 - pos._2 - 1) * (blockSize + blockMargin),
        offset._1 + pos._1 * (blockSize + blockMargin) + blockSize,
        offset._2 + (gridSize._2 - pos._2 - 1) * (blockSize + blockMargin) + blockSize)
    def drawEmptyGrid {
      for {
        x <- 0 to gridSize._1 - 1
        y <- 0 to gridSize._2 - 1
        val pos = (x, y)
      } g drawRect (buildRect(pos), bluishLigherGray)      
    }
    def drawBlocks {
      blocks filter {_.pos._2 < gridSize._2} foreach { b =>
        g drawRect (buildRect(b.pos), bluishEvenLigher) }
    }
    def drawCurrent {
      current filter {_.pos._2 < gridSize._2} foreach { b =>
        g drawRect (buildRect(b.pos), bluishSilver) }
    }
    drawEmptyGrid
    drawBlocks
    drawCurrent  
  }
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
  def withCanvas(f: Canvas => Unit) {
    val canvas = holder.lockCanvas(null)
    try {
      f(canvas)
    } finally {
      holder.unlockCanvasAndPost(canvas)
    }
  }
}
