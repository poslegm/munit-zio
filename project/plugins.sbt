addSbtPlugin("org.scalameta"  % "sbt-scalafmt"   % "2.5.4")
addSbtPlugin("com.github.sbt" % "sbt-ci-release" % "1.11.2")

val scalaJSVersion          = sys.env.getOrElse("SCALAJS_VERSION", "1.19.0")
val scalaNativeVersion      = sys.env.getOrElse("SCALA_NATIVE_VERSION", "0.5.7")
addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject"      % "1.3.2")
addSbtPlugin("org.portable-scala" % "sbt-scala-native-crossproject" % "1.3.2")
addSbtPlugin("org.scala-js"       % "sbt-scalajs"                   % scalaJSVersion)
addSbtPlugin("org.scala-native"   % "sbt-scala-native"              % scalaNativeVersion)
