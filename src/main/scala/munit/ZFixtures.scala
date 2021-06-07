package munit

import zio.{IO, Managed, UIO, Exit, ZIO}

trait ZFixtures {
  self: ZSuite =>

  /** Test-local fixture.
    *
    * Can be created from raw setup/teardown effects or from ZManaged.
    *
    * {{{
    * val rawZIOFunFixture = ZTestLocalFixture(options => ZIO.succeed(s"acquired ${options.name}")) { str =>
    *   putStrLn(s"cleanup [$str]").provideLayer(Console.live)
    * }
    *
    * val ZManagedFunFixture = ZTestLocalFixture { options =>
    *   ZManaged.make(ZIO.succeed(s"acquired ${options.name} with ZManaged")) { str =>
    *     putStrLn(s"cleanup [$str] with ZManaged").provideLayer(Console.live).orDie
    *   }
    * }
    *
    * rawZIOFunFixture.test("allocate resource with ZIO FunFixture") { str =>
    *   assertNoDiff(str, "acquired allocate resource with ZIO FunFixture")
    * }
    *
    * ZManagedFunFixture.test("allocate resource with ZManaged FunFixture") { str =>
    *   assertNoDiff(str, "acquired allocate resource with ZManaged FunFixture with ZManaged")
    * }
    * }}}
    */
  object ZTestLocalFixture {
    def apply[E, A](setup: TestOptions => IO[E, A])(teardown: A => IO[E, Unit]): FunFixture[A] =
      FunFixture.async(
        options => unsafeRunToFuture(setup(options)),
        t => unsafeRunToFuture(teardown(t))
      )

    def apply[E, A](create: TestOptions => Managed[E, A]): FunFixture[A] = {
      var release: Exit[Any, Any] => UIO[Any] = null
      FunFixture.async(
        setup = { options =>
          val effect = for {
            res      <- create(options).reserve
            _        <- ZIO.effectTotal { release = res.release }
            resource <- res.acquire
          } yield resource
          unsafeRunToFuture(effect)
        },
        teardown = { resource =>
          val effect = release(Exit.succeed(resource)).unit
          unsafeRunToFuture(effect)
        }
      )
    }
  }

  /** Suite local fixture from ZManaged.
    *
    * {{{
    *
    * var state   = 0
    * val fixture = ZSuiteLocalFixture(
    *   "sample",
    *   ZManaged.make(ZIO.effectTotal { state += 1; state })(_ => ZIO.effectTotal { state -= 1 })
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
    private case class Resource[T](content: T, release: Exit[Any, Any] => UIO[Any])

    def apply[E, A](name: String, managed: Managed[E, A]): Fixture[A] = {
      var resource: Resource[A] = null
      new Fixture[A](name) {
        def apply(): A =
          if (resource == null) throw new FixtureNotInstantiatedException(name)
          else resource.content

        override def beforeAll(): Unit = {
          val effect = for {
            res     <- managed.reserve
            content <- res.acquire
            _       <- ZIO.effectTotal { resource = Resource(content, res.release) }
          } yield ()
          runtime.unsafeRun(effect)
        }

        override def afterAll(): Unit = {
          runtime.unsafeRun(resource.release(Exit.succeed(resource.content)))
          ()
        }
      }
    }
  }
}
