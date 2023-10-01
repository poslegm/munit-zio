addSbtPlugin("org.scalameta"  % "sbt-scalafmt"   % "2.5.0")
addSbtPlugin("com.github.sbt" % "sbt-ci-release" % "1.5.12")

val scalaJSVersion = sys.env.getOrElse("SCALAJS_VERSION", "1.14.0")
addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "1.2.0")
addSbtPlugin("org.scala-js"       % "sbt-scalajs"              % scalaJSVersion)
