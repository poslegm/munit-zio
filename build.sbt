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

val scala212 = "2.12.21"
val scala213 = "2.13.18"
val scala3   = "3.9.0"

lazy val Version = new {
  val munit         = "1.3.6"
  val zio           = "2.1.27"
  val scalaJavaTime = "2.11.0"
}

commands += Command.command("ci-test") { s =>
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
      "-deprecation",
      "-unchecked"
    ),
    scalacOptions ++= {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, 12)) =>
          Seq("-Xfatal-warnings", "-Xsource:3")
        case Some((2, 13)) =>
          Seq(
            "-Xfatal-warnings",
            "-Xsource:3",
            "-Wdead-code",
            "-Wextra-implicit",
            "-Wunused",
            "-Ywarn-value-discard"
          )
        case _             => Seq("-Werror", "-explain", "-source:3.0-migration")
      }
    },
    crossScalaVersions                     := Seq(scala3, scala212, scala213),
    libraryDependencies ++= Seq(
      "org.scalameta" %%% "munit" % Version.munit,
      "dev.zio"       %%% "zio"   % Version.zio
    ),
    testFrameworks += new TestFramework("munit.Framework")
  )
  .jvmSettings(scalacOptions ++= Seq("-release", "17"))
  .jsSettings(crossSettings)
  .nativeSettings(crossSettings)

def crossSettings: List[Setting[?]] = List(
  Test / parallelExecution := false, // NOTE: https://scalameta.org/munit/docs/tests.html#run-tests-in-parallel
  libraryDependencies +=
    "io.github.cquiroz"   %%% "scala-java-time" % Version.scalaJavaTime % Test // To run the tests in JSPlatform
)

addCommandAlias("fmt", """scalafmtSbt;scalafmtAll""")
addCommandAlias("fmtCheck", """scalafmtSbtCheck;scalafmtCheckAll""")
