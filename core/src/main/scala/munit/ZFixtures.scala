package munit

import zio.*

trait ZFixtures {
  self: ZSuite =>

  /** Test-local fixture.
    *
    * Can be created from raw setup/teardown effects or from Scoped effect.
    *
    * {{{
    * val rawZIOFunFixture = ZTestLocalFixture(options => ZIO.succeed(s"acquired \${options.name}")) { str =>
    *   putStrLn(s"cleanup [\$str]").provideLayer(Console.live)
    * }
    *
    * val scopedFunFixture = ZTestLocalFixture { options =>
    *   ZIO.acquireRelease(ZIO.succeed(s"acquired \${options.name} with Scoped")) { str =>
    *     printLine(s"cleanup [\$str] with Scoped").orDie
    *   }
    * }
    *
    * rawZIOFunFixture.test("allocate resource with ZIO FunFixture") { str =>
    *   assertNoDiff(str, "acquired allocate resource with ZIO FunFixture")
    * }
    *
    * scopedFunFixture.test("allocate resource with Scoped FunFixture") { str =>
    *   assertNoDiff(str, "acquired allocate resource with Scoped FunFixture with Scoped")
    * }
    * }}}
    */
  object ZTestLocalFixture {
    def apply[E, A](setup: TestOptions => IO[E, A])(teardown: A => IO[E, Unit]): FunFixture[A] =
      FunFixture.async(
        options => unsafeRunToFuture(setup(options)),
        t => unsafeRunToFuture(teardown(t))
      )

    def apply[E, A](create: TestOptions => ZIO[Scope, E, A]): FunFixture[A] = {
      val scope = Unsafe.unsafe { implicit unsafe =>
        runtime.unsafe.run(Scope.make).getOrThrow()
      }
      FunFixture.async(
        setup = { options =>
          unsafeRunToFuture(create(options).provideLayer(ZLayer.succeed(scope)))
        },
        teardown = { resource =>
          val effect = scope.close(Exit.succeed(resource)).unit
          unsafeRunToFuture(effect)
        }
      )
    }
  }

  /** Suite local fixture from Scoped effect.
    *
    * {{{
    *
    * var state   = 0
    * val fixture = ZSuiteLocalFixture(
    *   "sample",
    *   ZIO.acquireRelease(ZIO.attempt { state += 1; state })(_ => ZIO.attempt { state -= 1 }.orDie)
    * )
    *
    * override val munitFixtures = Seq(fixture)
    *
    * test("suite local fixture works") {
    *   assertEquals(fixture(), 1)
    * }
    * }}}
    */
  object ZSuiteLocalFixture {
    final class FixtureNotInstantiatedException(name: String)
        extends Exception(
          s"The fixture `$name` was not instantiated. Override `munitFixtures` and include a reference to this fixture."
        )

    def apply[E, A](name: String, managed: ZIO[Scope, E, A]): Fixture[A] = {
      var resource: Option[A] = None

      val scope = Unsafe.unsafe { implicit unsafe =>
        runtime.unsafe.run(Scope.make).getOrThrow()
      }
      new Fixture[A](name) {
        def apply(): A =
          resource.getOrElse(throw new FixtureNotInstantiatedException(name))

        override def beforeAll(): Unit = {
          Unsafe.unsafe { implicit unsafe =>
            runtime.unsafe.run(
              managed.map { r => resource = Some(r) }.provideLayer(ZLayer.succeed(scope))
            )
          }
          ()
        }

        override def afterAll(): Unit = {
          Unsafe.unsafe { implicit unsafe =>
            runtime.unsafe.run(scope.close(Exit.succeed(resource)))
          }
          ()
        }
      }
    }
  }
}
