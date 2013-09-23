---
out: println-debugging.html
---

  [adb]: http://developer.android.com/tools/help/adb.html

### println デバッグ

何が起こっているのか調査するために、library の中の println文をいくつかアンコメントしてみる。

次に `\$ANDROID_HOME/platform-tools/` から `adb -e shell` を実行してエミュレータ内で adb シェルのセッションを開始する。詳細は [Android Debug Bridge][adb] を参照。次に adb シェル内から:

```
root@android:/ # logcat System.out:D *:S
I/System.out(  873): bestMove took 13464 ms
I/System.out(  873): selected List(MoveLeft, Drop) -0.03301514803843836
I/System.out(  873): bestMove took 12045 ms
I/System.out(  873): selected List(MoveLeft, Drop) -0.03301514803843836
I/System.out(  873): bestMove took 10781 ms
I/System.out(  873): selected List(MoveLeft, Drop) -0.03301514803843836
```

これでアプリ内の println の呼び出しが画面にストリームされるようになった。

次の一手を計算するのに 10秒以上かかっていて、これは現在計算の要請を行っているペースをかなり下回っている。これだけ遅れると次の一手は古い状態に基いて計算されることになり、最新の状態に対しては酷い一手の推論となる。これを回避するには次の一手の計算を GameMasterActor のループ内でブロックさせることだ:

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

`./adb -d shell` を用いて接続された携帯内での adb シェルセッションを開始する。

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

まともな計算結果になってきたけど、携帯でも結構時間がかかっている。
