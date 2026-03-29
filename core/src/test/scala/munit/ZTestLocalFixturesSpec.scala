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

  // Regression test: scope must be fresh per test, not shared across tests
  val activeResources = new java.util.concurrent.atomic.AtomicInteger(0)

  val scopeReuseFixture = ZTestLocalFixture { _ =>
    ZIO.acquireRelease(
      ZIO.succeed(activeResources.incrementAndGet())
    )(_ => ZIO.succeed(activeResources.decrementAndGet()))
  }

  scopeReuseFixture.test("scope reuse - first test resource is active") { _ =>
    assertEquals(activeResources.get(), 1)
  }

  scopeReuseFixture.test("scope reuse - second test resource is also active") { _ =>
    // FAILS with the bug: scope from first test was closed in teardown,
    // so finalizer runs immediately during setup of second test → counter == 0
    assertEquals(activeResources.get(), 1)
  }
}
