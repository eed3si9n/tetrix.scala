lazy val buildSettings = Seq(
  version := "0.2.0-SNAPSHOT",
  organization := "com.eed3si9n",
  homepage := Some(url("http://eed3si9n.com")),
  licenses := Seq("MIT License" -> url("http://opensource.org/licenses/mit-license.php/")),
  scalaVersion := "2.10.2",
  scalacOptions := Seq("-deprecation", "-unchecked"),
  resolvers += Resolver.sonatypeRepo("public")
)

lazy val swingDependencies = Def.setting {
  "org.scala-lang" % "scala-swing" % scalaVersion.value
}

lazy val root = (project in file(".")).
  settings(buildSettings: _*).
  settings(name := "tetrix.scala")

lazy val library = (project in file("library")).
  settings(buildSettings: _*)

lazy val swing = (project in file("swing")).
  settings(buildSettings: _*).
  settings(
    fork in run := true,
    libraryDependencies += swingDependencies.value
  ).
  dependsOn(library)
