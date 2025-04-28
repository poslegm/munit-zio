inThisBuild(
  List(
    organization := "com.github.poslegm",
    homepage     := Some(url("https://github.com/poslegm/munit-zio/")),
    licenses     := List("MIT" -> url("http://opensource.org/licenses/MIT")),
    developers   := List(
      Developer(
        "poslegm",
        "Mikhail Chugunkov",
        "poslegm@gmail.com",
        url("https://github.com/poslegm")
      )
    )
  )
)

val scala212 = "2.12.20"
val scala213 = "2.13.16"
val scala3   = "3.3.5"

lazy val Version = new {
  val munit         = "1.1.0"
  val zio           = "2.1.17"
  val scalaJavaTime = "2.6.0"
}

commands += Command.command("ci-test") { s =>
  val scalaVersion = sys.env.get("TEST") match {
    case Some("2.12") => scala212
    case Some("2.13") => scala213
    case _            => scala3
  }
  s"++$scalaVersion" ::
    "test" ::
    "publishLocal" ::
    s
}

lazy val core = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .withoutSuffixFor(JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("core"))
  .settings(
    name                                   := "munit-zio",
    libraryDependencies += "com.github.sbt" % "junit-interface" % "0.13.3" % "test",
    scalaVersion                           := scala3,
    scalacOptions ++= Seq(
      "-Xfatal-warnings",
      "-deprecation",
      "-unchecked"
    ),
    scalacOptions ++= {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, 12)) =>
          Seq("-Xsource:3")
        case Some((2, 13)) =>
          Seq(
            "-Xsource:3",
            "-Wdead-code",
            "-Wextra-implicit",
            "-Wunused",
            "-Ywarn-value-discard"
          )
        case _             => Seq("-explain", "-source:3.0-migration")
      }
    },
    crossScalaVersions                     := Seq(scala3, scala212, scala213),
    libraryDependencies ++= Seq(
      "org.scalameta" %%% "munit" % Version.munit,
      "dev.zio"       %%% "zio"   % Version.zio
    ),
    testFrameworks += new TestFramework("munit.Framework")
  )
  .jsSettings(crossSettings)
  .nativeSettings(crossSettings)

def crossSettings: List[Setting[?]] = List(
  Test / parallelExecution := false, // NOTE: https://scalameta.org/munit/docs/tests.html#run-tests-in-parallel
  libraryDependencies +=
    "io.github.cquiroz"   %%% "scala-java-time" % Version.scalaJavaTime % Test // To run the tests in JSPlatform
)

addCommandAlias("fmt", """scalafmtSbt;scalafmtAll""")
addCommandAlias("fmtCheck", """scalafmtSbtCheck;scalafmtCheckAll""")
