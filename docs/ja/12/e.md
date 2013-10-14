---
out: loading-on-device.html
---

### デバイスへの読み込み

エミュレータに読み込むためには、device を選択して `android:run` を実行する:

```
tetrix_droid> devices
[info] Connected devices:
[info]   emulator-5554          test_galaxynexus
tetrix_droid> device emulator-5554
[info] default device: emulator-5554
tetrix_droid> android:run
[info] Generating R.java
[info] [debug] cache hit, skipping proguard!
[info] classes.dex is up-to-date
[info] Debug package does not need signing: tetrix_droid-debug-unaligned.apk
[info] zipaligned: tetrix_droid-debug.apk
[info] Installing...
```

エミュレータに表示された。

![day12c](../files/tetrix-in-scala-day12c.png)

実際の動作を確認するために実機に載せてみよう。これは僕の htc one で実行された tetrix だ:

![day12d](../files/tetrix-in-scala-day12d.jpg)

エージェントは動いてはいるが、すごい悪手ばかり選んでくる。

続きはまた明日。

```
\$ git fetch origin
\$ git co day12v2 -b try/day12
\$ sbt swing/run
```
