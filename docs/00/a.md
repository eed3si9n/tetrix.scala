
### swing

Now let's write swing.

```scala
package com.tetrix.swing

import swing._
import event._

object Main extends SimpleSwingApplication {
  import event.Key._
  import java.awt.{Dimension, Graphics2D, Graphics, Image, Rectangle}
  import java.awt.{Color => AWTColor}

  val bluishGray = new AWTColor(48, 99, 99)
  val bluishSilver = new AWTColor(210, 255, 255)

  def onKeyPress(keyCode: Value) = keyCode match {
    case _ => // do something
  }
  def onPaint(g: Graphics2D) {
    // paint something
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
  }
}
```

I did glance a bit of [The scala.swing package](http://www.scala-lang.org/sites/default/files/sids/imaier/Mon,%202009-11-02,%2008:55/scala-swing-design.pdf), but I took most of the above from my first Tetrix implemention.
scala swing implements a bunch of setter methods (`x_=`) so we can write `x = "foo"` strait in class body. It's almost refershing to see how proudly mutable this framework is, and I think it works here since UI is one big side effect anyway. 

### abstract UI

I don't want to be tied to swing, but there aren't much difference among the platforms. Mostly you have some screen and input to move blocks around. So, the player or the timer takes actions that changes the state of the game, and the result is displayed on the screen. For now, let's approximate the state using a `String` var.

```scala
package com.eed3si9n.tetrix

class AbstractUI {
  private[this] var lastKey: String = ""

  def left() {
    lastKey = "left"
  }
  def right() {
    lastKey = "right"
  }
  def up() {
    lastKey = "up"
  }
  def down() {
    lastKey = "down"
  }
  def space() {
    lastKey = "space"
  }
  def last: String = lastKey
}
```

We can hook this up to the swing UI as follows:

```scala
  import com.eed3si9n.tetrix._

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
    g setColor bluishSilver
    g drawString (ui.last, 20, 20)
  }  
```

So now, we have an exciting game that displays `"left"` when you hit left arrow key.
I think this is good enough for the first day.

To run this on your machine,

```
\$ git clone https://github.com/eed3si9n/tetrix.scala.git
\$ cd tetrix.scala
\$ git co day0v2 -b try/day0
\$ sbt swing/run
```
