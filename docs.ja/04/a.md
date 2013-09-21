---
out: concurrency.html
---

  [amazon]: http://www.amazon.co.jp/dp/4798125415
  [amazon2]: http://www.amazon.co.jp/exec/obidos/ASIN/4797337206/tyano-22/

### 並行処理

Goetz の Java Concurrency in Practice ([Java並行処理プログラミング][amazon2]) を引用すると:

> スレッドセーフなコードを書くということは、その本質において、**状態**、特に**共有された可変状態**へのアクセスを管理することにある。

調度良いことに、僕達は既に tetrix の中身をリファクタリングして、それぞれの操作はある `GameState` から別の状態への遷移関数であるように書き換えた。以下に簡易化した `AbstractUI` を見てみる:

```scala
package com.eed3si9n.tetrix

class AbstractUI {
  import Stage._
  import java.{util => ju}
  
  private[this] var state = newState(...)
  private[this] val timer = new ju.Timer
  timer.scheduleAtFixedRate(new ju.TimerTask {
    def run { state = tick(state) }
  }, 0, 1000)
  def left() {
    state = moveLeft(state)
  }
  def right() {
    state = moveRight(state)
  }
  def view: GameView = state.view
}
```

タイマーは `tick(state)` を呼び出して `state` を変更し、プレーヤーもまた `moveLeft(state)` や `moveRight(state)` を呼び出して `state` を変更することができる。これは教科書に出てくるようなスレッド・アンセーフな例だ。以下にタイマースレッドと swing のイベントディスパッチスレッドの不幸な実行例を見てみる:

```
タイマースレッド: 共有された state を読み込む。現在のピースは (5, 18) にある
イベントスレッド: 共有された state を読み込む。現在のピースは (5, 18) にある
タイマースレッド: tick() 関数を呼び出す
タイマースレッド: tick() は現在のピースが (5, 17) にある新しい状態を返す
イベントスレッド: moveLeft() 関数を呼び出す
イベントスレッド: moveLeft() は現在のピースが (4, 18) にある新しい状態を返す
イベントスレッド: 新しい状態を共有された state に書き込む。現在のピースは (4, 18) にある
タイマースレッド: 新しい状態を共有された state に書き込む。現在のピースは (5, 17) にある
```

プレーヤーから見ると、左への動きが完全に無視されたか、もしくはピースが一瞬 `(4, 18)` から `(5, 17)` へ斜めへジャンプしたように見える。これが競合状態だ。

### synchronized

この場合、各タスクが短命で、かつシンプルな可変性のため、`state` に同期をかけるだけでうまくいくかもしれない。

```scala
package com.eed3si9n.tetrix

class AbstractUI {
  import Stage._
  import java.{util => ju}
  
  private[this] var state = newState(...)
  private[this] val timer = new ju.Timer
  timer.scheduleAtFixedRate(new ju.TimerTask {
    def run { updateState {tick} }
  }, 0, 1000)
  def left()  = updateState {moveLeft}
  def right() = updateState {moveRight}
  def view: GameView = state.view
  private[this] def updateState(trans: GameState => GameState) {
    synchronized {
      state = trans(state)
    }
  }
}
```

`synchronized` 節を用いることで、`state` の読み込みと書き込みが atomic に行われることが保証される。この方法はもし可変性が広範囲に渡っていたり、バックグラウンドでの長期のタスクが必要な場合は実用的じゃないかもしれない。
