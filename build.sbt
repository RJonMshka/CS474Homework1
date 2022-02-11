ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.1.1"

libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.11" % "test"


lazy val root = (project in file("."))
  .settings(
    name := "CS474Homework1"

  )
