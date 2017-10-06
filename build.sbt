val scalaV211 = "2.11.11"
val scalaV212 = "2.12.3"
crossScalaVersions in ThisBuild := Seq(scalaV211, scalaV212)
scalaVersion in ThisBuild := scalaV212

organization := "io.github.easel"
name := "utils-akka"
version := "0.0.1"
isSnapshot := version.value.contains("-SNAPSHOT")
val akkaVersion = "2.5.6"
libraryDependencies += "com.typesafe.akka" %% "akka-actor" % akkaVersion
libraryDependencies += "com.typesafe.akka" %% "akka-stream" % akkaVersion
libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test"
libraryDependencies += "com.github.pathikrit" %% "better-files" % "2.17.1"
libraryDependencies += "org.scalactic" %% "scalactic" % "3.0.1"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.1" % "test"

