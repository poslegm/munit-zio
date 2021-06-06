package munit

import zio.{Task, TaskManaged, UIO, Exit, ZIO}

trait ZFixtures:
  self: ZSuite =>

  /** Test-local fixture.
    *
    * Can be created from raw setup/teardown effects or from ZManaged.
    *
    * {{{
    * val rawZIOFunFixture = ZFunFixture(options => ZIO.succeed(s"acquired ${options.name}")) { str =>
    *   putStrLn(s"cleanup [$str]").provideLayer(Console.live)
    * }
    *
    * val ZManagedFunFixture = ZFunFixture { options =>
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
  object ZFunFixture:
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
