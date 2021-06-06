package munit

import zio.*
import zio.console.*

class ZTestLocalFixturesSpec extends ZSuite:
  val rawZIOFunFixture = ZTestLocalFixture(options => ZIO.succeed(s"acquired ${options.name}")) {
    str =>
      putStrLn(s"cleanup [$str]").provideLayer(Console.live)
  }

  val ZManagedFunFixture = ZTestLocalFixture { options =>
    ZManaged.make(ZIO.succeed(s"acquired ${options.name} with ZManaged")) { str =>
      putStrLn(s"cleanup [$str] with ZManaged").provideLayer(Console.live).orDie
    }
  }

  rawZIOFunFixture.test("allocate resource with ZIO FunFixture") { str =>
    assertNoDiff(str, "acquired allocate resource with ZIO FunFixture")
  }

  ZManagedFunFixture.test("allocate resource with ZManaged FunFixture") { str =>
    assertNoDiff(str, "acquired allocate resource with ZManaged FunFixture with ZManaged")
  }

  FunFixture.map2(rawZIOFunFixture, ZManagedFunFixture).test("compose ZIO FunFixtures") {
    (str1, str2) =>
      assertNoDiff(str1, "acquired compose ZIO FunFixtures")
      assertNoDiff(str2, "acquired compose ZIO FunFixtures with ZManaged")
  }
