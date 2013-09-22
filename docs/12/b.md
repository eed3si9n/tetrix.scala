---
out: android.html
---

  [1]: http://developer.android.com/sdk/installing/index.html
  [pfn]: https://github.com/pfn/android-sdk-plugin
  [2]: https://github.com/gseitz/DiningAkkaDroids

## Android

It would be cool if we can show this game off on a cellphone, so let's port this to Android. First [install Android SDK][1] or update to the latest SDK if you can't remember the last time you updated it by launching `android` tool from command line:

```
\$ android
```

As of September 2013, the latest SDK is Android 4.3 (API 18), but I'm going to also download Android 4.1.2 (API 16) that's the oldest Jelly Bean. Next, create an Android Virtual Device (AVD) using API 16.

### pfn/android-plugin

Next we need [pfn/android-sdk-plugin][pfn] for sbt.

Create `project/android.sbt` and add the following:

```scala
addSbtPlugin("com.hanhuy.sbt" % "android-sdk-plugin" % "1.0.6")
```

Make the following changes to `build.sbt`:

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
    name := "tetrix_droid",
    platformTarget in Android := "android-16",
    proguardOptions in Android ++= Seq("-dontwarn sun.misc.Unsafe",
      """-keep class akka.** {
        |  public <methods>;
        |}""".stripMargin)
  ).
  dependsOn(library)
```

After reloading sbt and navigating to droid project, `devices`, `device`, and `android:run` tasks are going to be available.
