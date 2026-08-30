addSbtPlugin("org.scalameta"  % "sbt-scalafmt"   % "2.5.6")
addSbtPlugin("com.github.sbt" % "sbt-ci-release" % "1.12.1")

val scalaJSVersion          = sys.env.getOrElse("SCALAJS_VERSION", "1.22.0")
val scalaNativeVersion      = sys.env.getOrElse("SCALA_NATIVE_VERSION", "0.5.12")
addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject"      % "1.4.0")
addSbtPlugin("org.portable-scala" % "sbt-scala-native-crossproject" % "1.4.0")
addSbtPlugin("org.scala-js"       % "sbt-scalajs"                   % scalaJSVersion)
addSbtPlugin("org.scala-native"   % "sbt-scala-native"              % scalaNativeVersion)
