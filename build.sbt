import Dependencies._

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

val scala212 = "2.12.14"
val scala213 = "2.13.6"
val scala3   = "3.0.0"

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

lazy val root = project
  .in(file("."))
  .settings(
    name                                 := "munit-zio",
    libraryDependencies += "com.novocode" % "junit-interface" % "0.11" % "test",
    scalaVersion                         := scala3,
    scalacOptions                        := Seq(
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
    crossScalaVersions                   := Seq(scala3, scala212, scala213),
    libraryDependencies                  := Seq(munit, zio),
    testFrameworks += new TestFramework("munit.Framework")
  )

addCommandAlias("fmt", """scalafmtSbt;scalafmtAll""")
addCommandAlias("fmtCheck", """scalafmtSbtCheck;scalafmtCheckAll""")
