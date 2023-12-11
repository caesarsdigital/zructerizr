import BuildHelper._
import Dependencies._

lazy val root = project
  .in(file("."))
  .settings(publish / skip := true, resolvers += "jitpack" at "https://jitpack.io")
  .aggregate(
    zworkspace
    // docs
  )
inThisBuild(
  List(
    organization     := "com.caesars",
    organizationName := "Caesars Digital",
    licenses := List(
      "MPL-2.0" -> url("https://www.mozilla.org/en-US/MPL/2.0/")
    ),
    developers := List(
      Developer(
        "brandon.Barker",
        "Brandon Barker",
        "brandon.barker@gmail.com",
        url("https://github.com/bbarker")
      )
    )
  )
)

addCommandAlias("fmt", "all scalafmtSbt scalafmt test:scalafmt")
addCommandAlias(
  "fix",
  "; all compile:scalafix test:scalafix; all scalafmtSbt scalafmtAll"
)
addCommandAlias(
  "check",
  "; scalafmtSbtCheck; scalafmtCheckAll; compile:scalafix --check; test:scalafix --check"
)

lazy val zworkspace = (project in file("zworkspace"))
  .settings(stdSettings("ztrucurizer-zworkspace"))
  // .settings(crossProjectSettings)
  .settings(buildInfoSettings("ztrucurizer.zworkspace"))
  .settings(
    crossScalaVersions := Versions.crossScalaVersions,
    libraryDependencies ++= Seq(zio, zioTest) ++ structurizrDependencies
  )
  .settings(testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"))
  .enablePlugins(BuildInfoPlugin)
