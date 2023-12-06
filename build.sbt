import Dependencies._

ThisBuild / scalaVersion := Versions.scala2
ThisBuild / version := "0.1.0"
ThisBuild / organization := "com.caesars"

lazy val root = (project in file("."))
  .settings(
    name := "ztructurizr",
    crossScalaVersions := Versions.crossScalaVersions,
    libraryDependencies ++= Seq(zio, zioTest) ++ structurizrDependencies,
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )
