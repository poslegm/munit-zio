import sbt._

object Dependencies {
  private val munitVersion = "0.7.29"
  private val zioVersion   = "1.0.13"

  lazy val munit = "org.scalameta" %% "munit" % munitVersion
  lazy val zio   = "dev.zio"       %% "zio"   % zioVersion
}
