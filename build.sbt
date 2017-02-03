scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream" % "2.4.15",

  // general stuff
  "org.scalatest" %% "scalatest" % "3.0.1" % Test,
  "io.github.amrhassan" %% "scalacheck-cats" % "0.3.2" % Test,
  "org.typelevel" %% "discipline" % "0.7.3" % Test
)

scalacOptions in ThisBuild ++= Seq("-unchecked", "-deprecation", "-feature")

