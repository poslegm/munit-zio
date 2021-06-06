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

  testZ("interceptFailure works (successful assertion)") {
    val zio = ZIO.fail(new IllegalArgumentException("BOOM!"))
    zio.interceptFailure[IllegalArgumentException]
  }

  testZ("interceptDefect works (successful assertion)") {
    val zio = ZIO.die(new IllegalArgumentException("BOOM!"))
    zio.interceptDefect[IllegalArgumentException]
  }

  testZ("interceptFailure works (failed assertion: different exception)".fail) {
    val zio = ZIO.fail(new Exception("BOOM!"))
    zio.interceptFailure[IllegalArgumentException]
  }

  testZ("interceptDefect works (failed assertion: different exception)".fail) {
    val zio = ZIO.die(new Exception("BOOM!"))
    zio.interceptFailure[IllegalArgumentException]
  }

  testZ("interceptFailure works (failed assertion: effect does not fail)".fail) {
    val zio = ZIO.succeed(42)
    zio.interceptFailure[IllegalArgumentException]
  }

  testZ("interceptDefect works (failed assertion: effect does not fail)".fail) {
    val zio = ZIO.succeed(42)
    zio.interceptDefect[IllegalArgumentException]
  }

  testZ("interceptDefect works (failed assertion: effect does not die)".fail) {
    val zio = ZIO.fail(new IllegalArgumentException("fail not die"))
    zio.interceptDefect[IllegalArgumentException]
  }

  testZ("interceptFailureMessage works (successful assertion)") {
    val zio = ZIO.fail(new IllegalArgumentException("BOOM!"))
    zio.interceptFailureMessage[IllegalArgumentException]("BOOM!")
  }

  testZ("interceptFailureMessage works (failed assertion: different exception)".fail) {
    val zio = ZIO.fail(new Exception("BOOM!"))
    zio.interceptFailureMessage[IllegalArgumentException]("BOOM!")
  }

  testZ("interceptFailureMessage works (failed assertion: different message)".fail) {
    val zio = ZIO.fail(new IllegalArgumentException("oops"))
    zio.interceptFailureMessage[IllegalArgumentException]("BOOM!")
  }

  testZ("interceptFailureMessage works (failed assertion: ZIO does not fail)".fail) {
    val zio = ZIO.succeed(42)
    zio.interceptFailureMessage[IllegalArgumentException]("BOOM!")
  }
