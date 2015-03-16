import sbt._
import sbt.Keys._

object Tonic2Build extends Build {

  lazy val tonic2 = Project(
    id = "tonic2",
    base = file("."),
    settings = Project.defaultSettings ++ Seq(
      name := "tonic2",
      organization := "tonic",
      version := "0.1-SNAPSHOT",
      scalaVersion := "2.11.6"
      // add other settings here
    )
  )
}
