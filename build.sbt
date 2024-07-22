import BuildHelper._
import Dependencies._

lazy val root = project
  .in(file("."))
  .settings(
    publish / skip := true,
    resolvers += "jitpack" at "https://jitpack.io",
    crossScalaVersions := Versions.crossScalaVersions,
    crossPaths         := true,
  )
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

addCommandAlias("fmt", "; root / scalafmtSbt; root / scalafmt; Test / scalafmt")
addCommandAlias(
  "fix",
  "; root / scalafix; Test / scalafix; root / scalafmtSbt; root / scalafmtAll"
)
addCommandAlias(
  "check",
  "; root / scalafmtSbtCheck; root / scalafmtCheckAll; root / scalafix --check; Test / scalafix --check"
)

lazy val zworkspace = (project in file("zworkspace"))
  .settings(stdSettings("ztructurizr-zworkspace"))
  // .settings(crossProjectSettings)
  .settings(buildInfoSettings("ztructurizr.zworkspace"))
  .settings(
    crossScalaVersions := Versions.crossScalaVersions,
    libraryDependencies ++= Seq(zio, zioStreams, zioTest) ++ structurizrDependencies
  )
  .settings(testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"))
  .enablePlugins(BuildInfoPlugin)
