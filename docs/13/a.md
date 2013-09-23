---
out: println-debugging.html
---

  [adb]: http://developer.android.com/tools/help/adb.html

### println debugging

To find out what's going on, uncomment some of the println statements in the library.

Then run `adb -e shell` from `\$ANDROID_HOME/platform-tools/` to start an adb shell session in the emulator. See [Android Debug Bridge][adb] for more details. Then from the adb shell:

```
root@android:/ # logcat System.out:D *:S
I/System.out(  873): bestMove took 13464 ms
I/System.out(  873): selected List(MoveLeft, Drop) -0.03301514803843836
I/System.out(  873): bestMove took 12045 ms
I/System.out(  873): selected List(MoveLeft, Drop) -0.03301514803843836
I/System.out(  873): bestMove took 10781 ms
I/System.out(  873): selected List(MoveLeft, Drop) -0.03301514803843836
```

This will stream println calls within the app to the screen.

It's taking 10 seconds to calculate the best move, which is significantly longer than the pace at which the request is coming in. The best move information could be quite stale if it's using old state, and that's likely a horrible move recommendation for the current state. The work around would be to block on the best move loop within GameMasterActor:

```scala
  def receive = {
    case Start => loop 
  }
  private[this] def loop {
    var s = getStatesAndJudge._2
    while (s.status == ActiveStatus) {
      val t0 = System.currentTimeMillis
      val future = (agentActor ? BestMove(getState2, maxThinkTime))(60 second)
      Await.result(future, 60 second)
      val t1 = System.currentTimeMillis
      if (t1 - t0 < minActionTime) Thread.sleep(minActionTime - (t1 - t0))
      s = getStatesAndJudge._2
    }
  }
```

If we use `./adb -d shell`, we can start an adb shell session in the connected phone.

```
I/System.out(32611): bestMove took 1582 ms
I/System.out(32611): selected List(Drop) 0.9113095270054331
I/System.out(32611): bestMove took 2025 ms
I/System.out(32611): selected List(Drop) 0.9113095270054331
I/System.out(32611): bestMove took 1416 ms
I/System.out(32611): selected List(RotateCW, MoveLeft, MoveLeft, MoveLeft, MoveLeft, Drop) 0.8973939572929547
I/System.out(32611): bestMove took 1483 ms
I/System.out(32611): selected List(MoveRight, MoveRight, MoveRight, MoveRight, Drop) 0.9022247475073575
```

Much better calculation, but it still takes quite a long time on the phone.
