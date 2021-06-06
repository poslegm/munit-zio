package munit

import zio.*
import zio.console.*

import SampleDependencies.*

// TODO simple resource management (raw ZManaged) TEST-LOCAL DONE
// TODO resource management on layers (init/clean)

// FunFixture with layer. syntax:
// layers.testZIO("") {
//   ... accepts RIO[Layers, Any]
// }
//
//
// resource test/suite local fixture with ZManaged (like in munit-cats-effect). syntax:
//
// suite local:
// val fixture = ZManagedFixture.make(...)(...)
// test("suite local") {
//   ... accepts fixture call for resource
// }
//
// test local:
// val fixture = ZManagedFixture.make(...)(...)
// fixture.testZ("") { resource =>
//   ...
// }

// TODO fixtures should be composed with map2 for layers/resources composition

class ZFixturesSpec extends ZSuite:
  test("simple layers providing") {
    val deps = ZLayer.succeed(A()) >>> ZLayer.fromService[A, B](B(_))

    val effect = ZIO.service[B].map(_.g)
    assertZ(effect.provideLayer(deps))
  }

  val rawZIOFunFixture = ZFunFixture(options => ZIO.succeed(s"acquired ${options.name}")) { str =>
    putStrLn(s"cleanup [$str]").provideLayer(Console.live)
  }

  val ZManagedFunFixture = ZFunFixture { options =>
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

object SampleDependencies:
  class A:
    def f: Boolean = false

  class B(a: A):
    export a.*
    def g: Boolean = !f
