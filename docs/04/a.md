---
out: concurrency.html
---

### concurrency

To quote Goetz's Java Concurrency in Practice,

> Writing thread-safe code is, at its core, about managing access to _state_, and in particular to _shared, mutable state_.

Conveniently we have refactored the inner working of tetrix so each operation is written as transition function from one `GameState` to another. Here's a simplified version of `AbstractUI`:

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

The timer modifies `state` by calling `tick(state)`, and the player can also modify it by calling `moveLeft(state)` or `moveRight(state)`. This is a textbook example of a thread-unsafe code. Here's an unlucky run of the timer thread and swing's event dispatch thread:

```
timer thread: reads shared state. current piece at (5, 18)
event thread: reads shared state. current piece at (5, 18)
timer thread: calls tick() function
timer thread: tick() returns a new state whose current piece is at (5, 17)
event thread: calls moveLeft() function
event thread: moveLeft() returns a new state whose current piece is at (4, 18)
event thread: writes the new state into shared state. current piece at (4, 18)
timer thread: writes the new state into shared state. current piece at (5, 17)
```

When the player sees this, either it would look like the left move was completely ignored, or witness the piece jumping diagnally from `(4, 18)` to `(5, 17)`. This is a race condition.

### synchronized

In this case, because each tasks are short-lived, and because the mutability is simple, we probably could get away with synchronizing on `state`.

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

Using the `synchronized` clause, reading of `state` and writing of `state` is now guaranteed to happen atomically. This approach may not be practical if mutability is spread out more widely, or if background execution of tasks are required.
