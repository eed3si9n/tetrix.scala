---
out: android.html
---

  [1]: http://developer.android.com/sdk/installing/index.html
  [pfn]: https://github.com/pfn/android-sdk-plugin
  [2]: https://github.com/gseitz/DiningAkkaDroids

## Android

このゲームを携帯に載せて友達に見せたら面白そうなので、Android に移植する。まず [Android SDK をインストール][1]するか、最後にいつアップデートしたか覚えてないようならばコマンドラインから `android` を起動して最新の SDK にアップデートする:

```
\$ android
```

2013年9月現在のところ最新の SDK は Android 4.3 (API 18) だけども、一番古い Jelly Bean である Android 4.1.2 (API 16) もダウンロードする。API 16 を使って Android Virtual Device (AVD) を作る。

### pfn/android-plugin

次は sbt の [pfn/android-sdk-plugin][pfn] だ。

`project/android.sbt` を作って以下を書く:

```scala
addSbtPlugin("com.hanhuy.sbt" % "android-sdk-plugin" % "1.0.6")
```

そして以下の変更を `build.sbt` に加える:

```scala
import android.Keys._

...

lazy val library = (project in file("library")).
  settings(buildSettings: _*).
  settings(
    name := "tetrix_library",
    libraryDependencies ++= libDeps.value,
    exportJars := true
  )

...

lazy val droid = (project in file("android")).
  settings(buildSettings: _*).
  settings(androidBuild: _*).
  settings(
    platformTarget in Android := "android-16",
    proguardOptions in Android ++= Seq("-dontwarn sun.misc.Unsafe",
      """-keep class akka.** {
        |  public <methods>;
        |}""".stripMargin)
  ).
  dependsOn(library)
```

sbt を再読み込みして droid プロジェクトへ行くと、`devices`、`device`、`android:run` などのタスクが利用可能になっている。
