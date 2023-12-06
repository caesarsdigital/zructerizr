import Dependencies._

ThisBuild / scalaVersion := Versions.scala2
ThisBuild / version := "0.1.0"
ThisBuild / organization := "com.caesars"
ThisBuild / Test / fork := true
ThisBuild / testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")

lazy val root = (project in file("."))
  .settings(
    name := "ztructurizr",
    crossScalaVersions := Versions.crossScalaVersions,
    libraryDependencies ++= Seq(zio, zioTest) ++ structurizrDependencies
  )
