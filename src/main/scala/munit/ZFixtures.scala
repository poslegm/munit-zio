package munit

import zio.{Task, TaskManaged, UIO, Exit, ZIO}

trait ZFixtures:
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
  object ZTestLocalFixture:
    def apply[T](setup: TestOptions => Task[T])(teardown: T => Task[Unit]): FunFixture[T] =
      FunFixture.async(
        options => runtime.unsafeRunToFuture(setup(options)),
        t => runtime.unsafeRunToFuture(teardown(t))
      )

    def apply[T](create: TestOptions => TaskManaged[T]): FunFixture[T] =
      var release: Exit[Any, Any] => UIO[Any] = null
      FunFixture.async(
        setup = { options =>
          val effect = for
            res      <- create(options).reserve
            _        <- ZIO.effectTotal { release = res.release }
            resource <- res.acquire
          yield resource
          runtime.unsafeRunToFuture(effect)
        },
        teardown = { resource =>
          val effect = release(Exit.succeed(resource)).unit

          runtime.unsafeRunToFuture(effect)
        }
      )

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
  object ZSuiteLocalFixture:
    final class FixtureNotInstantiatedException(name: String)
        extends Exception(
          s"The fixture `$name` was not instantiated. Override `munitFixtures` and include a reference to this fixture."
        )
    private case class Resource[T](content: T, acquire: Exit[Any, Any] => UIO[Any])

    def apply[T](name: String, managed: TaskManaged[T]): Fixture[T] =
      var resource: Resource[T] = null
      new Fixture[T](name) {
        def apply(): T =
          if resource == null then throw FixtureNotInstantiatedException(name) else resource.content

        override def beforeAll(): Unit =
          val effect = for
            res     <- managed.reserve
            content <- res.acquire
            _       <- ZIO.effectTotal { resource = Resource(content, res.release) }
          yield ()
          runtime.unsafeRun(effect)

        override def afterAll(): Unit =
          runtime.unsafeRun(resource.acquire(Exit.succeed(resource.content)))
      }
