import sbt._

object Dependencies {
  object Versions {
    val scala2             = "2.13.14"
    val scala3             = "3.3.3"
    val crossScalaVersions = Seq(scala2, scala3)

    val silencer = "1.7.17"
    val zio      = "2.1.6"
  }

  val zio        = "dev.zio" %% "zio"          % Versions.zio
  val zioStreams = "dev.zio" %% "zio-streams"  % Versions.zio
  val zioTest    = "dev.zio" %% "zio-test"     % Versions.zio % Test
  val zioTestSbt = "dev.zio" %% "zio-test-sbt" % Versions.zio % Test

  val structurizrDependencies = Seq(
    // "com.structurizr" % "structurizr-client" % "1.20.1",
    "com.structurizr" % "structurizr-import" % "1.2.1",
    "com.structurizr" % "structurizr-export" % "1.18.0",
    "com.structurizr" % "structurizr-dsl"    % "1.34.0"
  )
}
