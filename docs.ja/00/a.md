
  [amazon]: http://www.amazon.co.jp/dp/4798125415

### swing

次に swing を書く。[Scala 逆引きレシピ][amazon]だと、「165: GUIアプリケーションを作りたい」が一応参考になるけど、ある程度 Java Swing を一緒に勉強する必要があると思う。

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

[The scala.swing package](http://www.scala-lang.org/sites/default/files/sids/imaier/Mon,%202009-11-02,%2008:55/scala-swing-design.pdf) もちらっと見たけど、上はだいたい前に書いた Tetrix の実装からもらってきた。
scala swing はセッターメソッド (`x_=`) をいくつも定義しているため、クラスの本体に直接 `x = "foo"` のように書くことができる。すがすがしいぐらいに可変 (mutable) なフレームワークだ。UI は全部副作用なので、これはうまくいっていると思う。

### 抽象 UI

あまり swing に縛られたくないが、特にプラットフォーム間で違いがあるわけでもない。だいたい画面があって、ブロックを動かすインプットがある。プレーヤーかタイマーがゲームがアクションを実行し、ゲームの状態が変わり、結果が画面に表示される。今のところは、ゲームの状態を `String` の var で代用しよう。

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

以下のようにして swing UI につなぐ:

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

これで、左矢印を押すと `"left"` と表示される面白いゲームができた。
初日はこんなものでいいんじゃないかな。

自分のマシンで試してみる手順:

```
$ git clone https://github.com/eed3si9n/tetrix.scala.git
$ cd tetrix.scala
$ git co day0v2 -b try/day0
$ sbt swing/run
```
