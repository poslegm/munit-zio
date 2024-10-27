addSbtPlugin("org.scalameta"  % "sbt-scalafmt"   % "2.5.2")
addSbtPlugin("com.github.sbt" % "sbt-ci-release" % "1.9.0")

val scalaJSVersion = sys.env.getOrElse("SCALAJS_VERSION", "1.16.0")
addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "1.3.2")
addSbtPlugin("org.scala-js"       % "sbt-scalajs"              % scalaJSVersion)
