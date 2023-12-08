addSbtPlugin("ch.epfl.scala" % "sbt-bloop" % "1.5.12")

addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.11.1")
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.10.0")

libraryDependencies += "org.snakeyaml" % "snakeyaml-engine" % "2.3"
addSbtPlugin("org.scalameta" % "sbt-mdoc" % "2.2.20")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.4.2")
addSbtPlugin("com.github.cb372" % "sbt-explicit-dependencies" % "0.2.15")
