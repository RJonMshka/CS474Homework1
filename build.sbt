ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.1.1"

libraryDependencies += "org.scalactic" %% "scalactic" % "3.2.11"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.11" % "test"
libraryDependencies += "org.scalatest" %% "scalatest-featurespec" % "3.2.11" % "test"

logBuffered in Test := false
parallelExecution in Test := false

lazy val root = (project in file("."))
  .settings(
    name := "CS474Homework1"
  )
