package munit

import zio.*
import zio.duration.*
import zio.clock.Clock

class ZAssertionsSpec extends ZSuite:

  testZ("assertZ works (successful assertion)") {
    val zio = ZIO.succeed(true)
    assertZ(zio)
  }

  testZ("assertZ works (failed assertion)".fail) {
    val zio = ZIO.succeed(false)
    assertZ(zio)
  }

  testZ("assertEqualsZ works (successful assertion)") {
    val zio = ZIO(41).map(_ + 1)
    assertEqualsZ(zio, 42)
  }

  testZ("assertEqualsZ works (failed assertion)".fail) {
    val zio = ZIO(41).map(_ + 2)
    assertEqualsZ(zio, 42)
  }

  testZ("assertNotEqualsZ works (successful assertion)") {
    val zio = ZIO(41).map(_ + 2)
    assertNotEqualsZ(zio, 42)
  }

  testZ("assertNotEqualsZ works (failed assertion)".fail) {
    val zio = ZIO(41).map(_ + 1)
    assertNotEqualsZ(zio, 42)
  }

  testZ("assertNoDiffZ works (successful assertion)") {
    val zio = ZIO.succeed("string")
    assertNoDiffZ(zio, "string")
  }

  testZ("assertNoDiffZ works (failed assertion)".fail) {
    val zio = ZIO.succeed("string")
    assertNoDiffZ(zio, "another string")
  }

  testZ("interceptFailureZ works (successful assertion)") {
    val zio = ZIO.fail(new IllegalArgumentException("BOOM!"))
    interceptFailureZ[IllegalArgumentException](zio)
  }

  testZ("interceptDefectZ works (successful assertion)") {
    val zio = ZIO.die(new IllegalArgumentException("BOOM!"))
    interceptDefectZ[IllegalArgumentException](zio)
  }

  testZ("interceptFailureZ works (failed assertion: different exception)".fail) {
    val zio = ZIO.fail(new Exception("BOOM!"))
    interceptFailureZ[IllegalArgumentException](zio)
  }

  testZ("interceptDefectZ works (failed assertion: different exception)".fail) {
    val zio = ZIO.die(new Exception("BOOM!"))
    interceptFailureZ[IllegalArgumentException](zio)
  }

  testZ("interceptFailureZ works (failed assertion: effect does not fail)".fail) {
    val zio = ZIO.succeed(42)
    interceptFailureZ[IllegalArgumentException](zio)
  }

  testZ("interceptDefectZ works (failed assertion: effect does not fail)".fail) {
    val zio = ZIO.succeed(42)
    interceptDefectZ[IllegalArgumentException](zio)
  }

  testZ("interceptDefectZ works (failed assertion: effect does not die)".fail) {
    val zio = ZIO.fail(new IllegalArgumentException("fail not die"))
    interceptDefectZ[IllegalArgumentException](zio)
  }

  testZ("interceptFailureMessageZ works (successful assertion)") {
    val zio = ZIO.fail(new IllegalArgumentException("BOOM!"))
    interceptFailureMessageZ[IllegalArgumentException]("BOOM!")(zio)
  }

  testZ("interceptFailureMessageZ works (failed assertion: different exception)".fail) {
    val zio = ZIO.fail(new Exception("BOOM!"))
    interceptFailureMessageZ[IllegalArgumentException]("BOOM!")(zio)
  }

  testZ("interceptFailureMessageZ works (failed assertion: different message)".fail) {
    val zio = ZIO.fail(new IllegalArgumentException("oops"))
    interceptFailureMessageZ[IllegalArgumentException]("BOOM!")(zio)
  }

  testZ("interceptFailureMessageZ works (failed assertion: IO does not fail)".fail) {
    val zio = ZIO.succeed(42)
    interceptFailureMessageZ[IllegalArgumentException]("BOOM!")(zio)
  }
