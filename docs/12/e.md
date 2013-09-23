---
out: loading-on-device.html
---

### loading on device

To load it on the emulator, select the device and `android:run`:

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

It did showed up on the emulator.

![day12c](http://eed3si9n.com/images/tetrix-in-scala-day12c.png)

To really get the feeling on how it works, let's load on a phone. Here's tetrix running on my htc one:

![day12d](http://eed3si9n.com/images/tetrix-in-scala-day12d.jpg)

The agent is playing, but it keeps picking really bad moves.

We'll pick it up from here tomorrow.

```
\$ git fetch origin
\$ git co day12v2 -b try/day12
\$ sbt swing/run
```
