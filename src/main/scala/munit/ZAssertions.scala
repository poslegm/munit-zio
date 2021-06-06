package munit

import zio.{ZIO, IO, Task, Exit}

import scala.reflect.ClassTag

trait ZAssertions:
  self: FunSuite =>

  /** Asserts that `Task[Boolean]` returns `true`.
    *
    * {{{
    *   test("false OR true should be true") {
    *     val effect = ZIO.succeed(false || true)
    *     assertZ(effect, "boolean algebra check failed")
    *   }
    * }}}
    *
    * @param cond
    *   the `Task[Boolean]` to be tested
    * @param clue
    *   a value that will be printed in case the assertions fail
    */
  final def assertZ(cond: Task[Boolean], clue: => Any = "assertion failed")(using
      Location
  ): Task[Unit] =
    cond.map(assert(_, clue))

  /** Asserts that `Task[String]` has no difference with expected string. Pretty prints diff unlike
    * just `assertEqualsZ`.
    *
    * {{{
    *   test("strings are the same") {
    *     val effect = ZIO.succeed("string")
    *     assertNoDiffZ(effect, "string", "different strings")
    *   }
    * }}}
    *
    * @param obtained
    *   the `Task[String]` to be tested
    * @param expected
    *   expected string
    * @param clue
    *   a value that will be printed in case the assertions fail
    */
  final def assertNoDiffZ(
      obtained: Task[String],
      expected: String,
      clue: => Any = "diff assertion failed"
  )(using loc: Location): Task[Unit] =
    obtained.map(assertNoDiff(_, expected, clue))

  /** Asserts that `Task[A]` returns the same result as expected
    * {{{
    *   test("strings are the same") {
    *     val effect = ZIO.succeed("string")
    *     assertEqualsZ(effect, "string", "different strings")
    *   }
    * }}}
    *
    * @param obtained
    *   the `Task[A]` to be tested
    * @param expected
    *   expected result
    * @param clue
    *   a value that will be printed in case the assertions fail
    */
  final def assertEqualsZ[A, B](
      obtained: Task[A],
      expected: B,
      clue: => Any = "values are not the same"
  )(using Location, B <:< A): Task[Unit] =
    obtained.map(assertEquals(_, expected, clue))

  /** Asserts that `Task[A]` returns NOT the same result as expected
    * {{{
    *   test("strings are the same") {
    *     val effect = ZIO.succeed("string")
    *     assertNotEqualsZ(effect, "another string", "same strings")
    *   }
    * }}}
    *
    * @param obtained
    *   the `Task[A]` to be tested
    * @param expected
    *   expected result
    * @param clue
    *   a value that will be printed in case the assertions fail
    */
  final def assertNotEqualsZ[A, B](
      obtained: Task[A],
      expected: B,
      clue: => Any = "values are not the same"
  )(using Location, A =:= B): Task[Unit] =
    obtained.map(assertNotEquals(_, expected, clue))

  /** Asserts that `Task[Any]` should fail with provided exception `E`.
    * {{{
    *   test("effect should fail") {
    *     val effect = ZIO.fail(new IllegalArgumentException("BOOM!"))
    *     interceptFailureZ[IllegalArgumentException](effect)
    *   }
    * }}}
    *
    * For "die" checking look at `interceptDefectZ`.
    *
    * @param body
    *   the `Task[Any]` to be tested
    */
  final def interceptFailureZ[E <: Throwable](
      body: Task[Any]
  )(using Location, ClassTag[E]): Task[E] =
    body.run.flatMap(runIntercept(None, _, false))

  /** Asserts that `Task[Any]` should die with provided exception `E`.
    * {{{
    *   test("effect should die") {
    *     val effect = ZIO.die(new IllegalArgumentException("BOOM!"))
    *     interceptDefectZ[IllegalArgumentException](effect)
    *   }
    * }}}
    *
    * For "fail" checking look at `interceptFailureZ`.
    *
    * @param body
    *   the `Task[Any]` to be tested
    */
  final def interceptDefectZ[E <: Throwable](
      body: Task[Any]
  )(using Location, ClassTag[E]): Task[E] =
    body.run.flatMap(runIntercept(None, _, true))

  /** Asserts that `Task[Any]` should fail with provided exception `E` and message `message`.
    * {{{
    *   test("effect should fail with message") {
    *     val effect = ZIO.fail(new IllegalArgumentException("BOOM!"))
    *     interceptFailureZ[IllegalArgumentException]("BOOM!")(effect)
    *   }
    * }}}
    *
    * @param body
    *   the `Task[Any]` to be tested
    */
  final def interceptFailureMessageZ[E <: Throwable](message: String)(
      body: Task[Any]
  )(using Location, ClassTag[E]): Task[E] =
    body.run.flatMap(runIntercept(Some(message), _, false))

  private def runIntercept[E <: Throwable](
      expectedExceptionMessage: Option[String],
      exit: Exit[Throwable, Any],
      shouldDie: Boolean
  )(using loc: Location, E: ClassTag[E]): Task[E] =
    exit match
      case Exit.Success(_)     =>
        Task(
          fail(
            s"expected exception of type '${E.runtimeClass.getName()}' but body evaluated successfully"
          )
        )
      case Exit.Failure(cause) =>
        val e = if shouldDie then cause.dieOption else cause.failureOption
        e match
          case Some(error: FailExceptionLike[_])
              if !E.runtimeClass.isAssignableFrom(error.getClass()) =>
            Task.fail(error)

          case Some(error) if E.runtimeClass.isAssignableFrom(error.getClass()) =>
            if expectedExceptionMessage.forall(_ == error.getMessage) then
              Task.succeed[E](error.asInstanceOf[E])
            else
              Task.fail {
                val obtained = error.getClass().getName()
                new FailException(
                  s"intercept failed, exception '$obtained' had message '${error.getMessage}', which was different from expected message '${expectedExceptionMessage.get}'",
                  cause = error,
                  isStackTracesEnabled = false,
                  location = loc
                )
              }

          case Some(error) =>
            Task.fail {
              val obtained = error.getClass().getName()
              val expected = E.runtimeClass.getName()
              new FailException(
                s"intercept failed, exception '$obtained' is not a subtype of '$expected",
                cause = error,
                isStackTracesEnabled = false,
                location = loc
              )
            }

          case None =>
            Task(
              fail(
                s"expected exception of type '${E.runtimeClass.getName()}' but body evaluated successfully"
              )
            )
