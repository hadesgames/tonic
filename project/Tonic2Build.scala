import sbt._
import sbt.Keys._

object Tonic2Build extends Build {

  lazy val tonic = Project(
    id = "tonic",
    base = file("tonic"),
    settings = Project.defaultSettings ++ Seq(
      name := "tonic",
      organization := "tonic",
      version := "0.1-SNAPSHOT",
      scalaVersion := "2.11.6",
      resolvers ++= repos,
      libraryDependencies ++= deps,
      javaOptions in run ++= Seq(
        "-Xmx250M"),
      fork := true
      // add other settings here
    )
  )

  lazy val client = Project(
    id = "client",
    base = file("client"),
    settings = Project.defaultSettings ++ Seq(
      name := "client",
      organization := "tonic",
      version := "0.1-SNAPSHOT",
      scalaVersion := "2.11.6",
      resolvers ++= repos,
      libraryDependencies ++= deps,
      javaOptions in run ++= Seq(
        "-Xmx250M"),
      fork := true
      // add other settings here
    )
  )

  lazy val repos = Seq(
    "spray repo" at "http://repo.spray.io"
  )
  lazy val deps = Seq(
    "io.spray" %% "spray-can" % "1.3.2",
    "io.spray" %% "spray-routing" % "1.3.2",
    "com.typesafe.akka" %% "akka-actor" % "2.3.6"
  )

}
