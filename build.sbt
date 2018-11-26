name := "Vespiquen"

version := "1.0"

scalaVersion := "2.12.6"

lazy val akkaVersion = "2.5.18"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
  "org.scalatest" %% "scalatest" % "3.0.5" % "test",
  "com.github.guillaumedd" %% "gstlib" % "0.1.2",
  "com.typesafe.akka" %% "akka-remote" % akkaVersion,
  "org.rogach" %% "scallop" % "3.1.5"
)
