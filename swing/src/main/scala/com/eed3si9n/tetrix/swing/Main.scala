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

    def buildRect(pos: (Int, Int)): Rectangle =
      new Rectangle(pos._1 * (blockSize + blockMargin),
        (view.gridSize._2 - pos._2 - 1) * (blockSize + blockMargin),
        blockSize, blockSize)
    def drawEmptyGrid {
      g setColor bluishLigherGray
      for {
        x <- 0 to view.gridSize._1 - 1
        y <- 0 to view.gridSize._2 - 2
        val pos = (x, y)
      } g draw buildRect(pos)      
    }
    def drawBlocks {
      g setColor bluishEvenLigher
      view.blocks foreach { b => g fill buildRect(b.pos) }
    }
    def drawCurrent {
      g setColor bluishSilver
      view.current foreach { b => g fill buildRect(b.pos) }
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
