package com.tetrix.swing

import com.eed3si9n.tetrix._
import swing._
import event._

object Main extends SimpleSwingApplication {
  import event.Key._
  import java.awt.{Dimension, Graphics2D, Graphics, Image, Rectangle}
  import java.awt.{Color => AWTColor}
  import javax.swing.{Timer => SwingTimer, AbstractAction}

  val bluishGray = new AWTColor(48, 99, 99)
  val bluishLigherGray = new AWTColor(79, 130, 130)
  val bluishEvenLigher = new AWTColor(145, 196, 196)
  val bluishSilver = new AWTColor(210, 255, 255)
  val blockSize = 16
  val blockMargin = 1

  val ui = new AbstractUI

  def onKeyPress(keyCode: Value) = keyCode match {
    case Left  => ui.left()
    case Right => ui.right()
    case Up    => ui.up()
    case Down  => ui.down()
    case Space => ui.space()
    case _ =>
  }
  def onPaint(g: Graphics2D) {
    val view = ui.view
    drawBoard(g, (0, 0), (10, 20), view.blocks, view.current)
    drawBoard(g, (12 * (blockSize + blockMargin), 0),
      view.miniGridSize, view.next, Nil)
    view.status match {
      case GameOver =>
        g setColor bluishSilver
        g drawString ("game over",
          12 * (blockSize + blockMargin), 7 * (blockSize + blockMargin))
      case _ => // do nothing
    }
  }
  def drawBoard(g: Graphics2D, offset: (Int, Int), gridSize: (Int, Int), 
      blocks: Seq[Block], current: Seq[Block]) {
    def buildRect(pos: (Int, Int)): Rectangle =
      new Rectangle(offset._1 + pos._1 * (blockSize + blockMargin),
        offset._2 + (gridSize._2 - pos._2 - 1) * (blockSize + blockMargin),
        blockSize, blockSize)
    def drawEmptyGrid {
      g setColor bluishLigherGray
      for {
        x <- 0 to gridSize._1 - 1
        y <- 0 to gridSize._2 - 1
        val pos = (x, y)
      } g draw buildRect(pos)      
    }
    def drawBlocks {
      g setColor bluishEvenLigher
      blocks filter {_.pos._2 < gridSize._2} foreach { b =>
        g fill buildRect(b.pos) }
    }
    def drawCurrent {
      g setColor bluishSilver
      current filter {_.pos._2 < gridSize._2} foreach { b =>
        g fill buildRect(b.pos) }
    }
    drawEmptyGrid
    drawBlocks
    drawCurrent  
  }

  def top = new MainFrame {
    title = "tetrix"
    contents = mainPanel
  }
  def mainPanel = new Panel {
    preferredSize = new Dimension(700, 400)
    focusable = true
    listenTo(keys)
    reactions += {
      case KeyPressed(_, key, _, _) =>
        onKeyPress(key)
        repaint
    }
    override def paint(g: Graphics2D) {
      g setColor bluishGray
      g fillRect (0, 0, size.width, size.height)
      onPaint(g)
    }
    val timer = new SwingTimer(100, new AbstractAction() {
      def actionPerformed(e: java.awt.event.ActionEvent) { repaint }
    })
    timer.start
  }
}
