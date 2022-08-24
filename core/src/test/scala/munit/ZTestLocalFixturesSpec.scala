package munit

import zio.*
import zio.Console.*

class ZTestLocalFixturesSpec extends ZSuite {
  val rawZIOFunFixture = ZTestLocalFixture(options => ZIO.succeed(s"acquired ${options.name}")) {
    str =>
      printLine(s"cleanup [$str]")
  }

  val ScopedFunFixture = ZTestLocalFixture { options =>
    ZIO.acquireRelease(ZIO.succeed(s"acquired ${options.name} with Scoped")) { str =>
      printLine(s"cleanup [$str] with Scoped").orDie
    }
  }

  rawZIOFunFixture.test("allocate resource with ZIO FunFixture") { str =>
    assertNoDiff(str, "acquired allocate resource with ZIO FunFixture")
  }

  ScopedFunFixture.test("allocate resource with Scoped FunFixture") { str =>
    assertNoDiff(str, "acquired allocate resource with Scoped FunFixture with Scoped")
  }

  FunFixture.map2(rawZIOFunFixture, ScopedFunFixture).test("compose ZIO FunFixtures") {
    case (str1, str2) =>
      assertNoDiff(str1, "acquired compose ZIO FunFixtures")
      assertNoDiff(str2, "acquired compose ZIO FunFixtures with Scoped")
  }
}
