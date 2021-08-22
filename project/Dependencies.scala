import sbt._

object Dependencies {
  private val munitVersion = "0.7.28"
  private val zioVersion   = "1.0.11"

  lazy val munit = "org.scalameta" %% "munit" % munitVersion
  lazy val zio   = "dev.zio"       %% "zio"   % zioVersion
}
