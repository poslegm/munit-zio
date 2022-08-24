package munit

import zio.{ZIO, IO, Exit}

import scala.reflect.ClassTag

trait ZAssertions {
  self: FunSuite =>

  /** Asserts that `ZIO[R, E, Boolean]` returns `true`.
    *
    * {{{
    *   testZ("false OR true should be true") {
    *     val effect = ZIO.succeed(false || true)
    *     assertZ(effect, "boolean algebra check failed")
    *   }
    * }}}
    *
    * @param cond
    *   the boolean effect to be tested
    * @param clue
    *   a value that will be printed in case the assertions fail
    */
  final def assertZ[R, E](cond: ZIO[R, E, Boolean], clue: => Any = "assertion failed")(implicit
      loc: Location
  ): ZIO[R, E, Unit] =
    cond.map(assert(_, clue))

  /** Asserts that `ZIO[R, E, String]` has no difference with expected string. Pretty prints diff
    * unlike just `assertEqualsZ`.
    *
    * {{{
    *   testZ("strings are the same") {
    *     val effect = ZIO.succeed("string")
    *     assertNoDiffZ(effect, "string", "different strings")
    *   }
    * }}}
    *
    * @param obtained
    *   the string effect to be tested
    * @param expected
    *   expected string
    * @param clue
    *   a value that will be printed in case the assertions fail
    */
  final def assertNoDiffZ[R, E](
      obtained: ZIO[R, E, String],
      expected: String,
      clue: => Any = "diff assertion failed"
  )(implicit loc: Location): ZIO[R, E, Unit] =
    obtained.map(assertNoDiff(_, expected, clue))

  /** Asserts that `ZIO[R, E, A]` returns the same result as expected
    * {{{
    *   testZ("strings are the same") {
    *     val effect = ZIO.succeed("string")
    *     assertEqualsZ(effect, "string", "different strings")
    *   }
    * }}}
    *
    * @param obtained
    *   the effect to be tested
    * @param expected
    *   expected result
    * @param clue
    *   a value that will be printed in case the assertions fail
    */
  final def assertEqualsZ[R, E, A, B](
      obtained: ZIO[R, E, A],
      expected: B,
      clue: => Any = "values are not the same"
  )(implicit loc: Location, ev: B <:< A): ZIO[R, E, Unit] =
    obtained.map(assertEquals(_, expected, clue))

  /** Asserts that `ZIO[R, E, A]` returns NOT the same result as expected
    * {{{
    *   testZ("strings are the same") {
    *     val effect = ZIO.succeed("string")
    *     assertNotEqualsZ(effect, "another string", "same strings")
    *   }
    * }}}
    *
    * @param obtained
    *   the effect to be tested
    * @param expected
    *   expected result
    * @param clue
    *   a value that will be printed in case the assertions fail
    */
  final def assertNotEqualsZ[R, E, A, B](
      obtained: ZIO[R, E, A],
      expected: B,
      clue: => Any = "values are not the same"
  )(implicit loc: Location, ev: A =:= B): ZIO[R, E, Unit] =
    obtained.map(assertNotEquals(_, expected, clue))

  implicit class InterceptSyntax[R, E <: Throwable](private val body: ZIO[R, E, Any]) {

    /** Asserts that `ZIO[R, E, Any]` should fail with provided exception `E`.
      * {{{
      *   testZ("effect should fail") {
      *     val effect = ZIO.fail(new IllegalArgumentException("BOOM!"))
      *     effect.interceptFailure[IllegalArgumentException]
      *   }
      * }}}
      *
      * For "die" checking look at `interceptDefect`.
      *
      * @param body
      *   the ZIO to be tested
      */
    def interceptFailure[E1 <: E](implicit
        loc: Location,
        ct: ClassTag[E1]
    ): ZIO[R, FailExceptionLike[?], E1] =
      body.exit.flatMap(runIntercept(None, _, false))

    /** Asserts that `ZIO[R, E, Any]` should die with provided exception `E`.
      * {{{
      *   testZ("effect should die") {
      *     val effect = ZIO.die(new IllegalArgumentException("BOOM!"))
      *     effect.interceptDefect[IllegalArgumentException]
      *   }
      * }}}
      *
      * For "fail" checking look at `interceptFailure`.
      *
      * @param body
      *   the effect to be tested
      */
    def interceptDefect[E1 <: Throwable](implicit
        loc: Location,
        ct: ClassTag[E1]
    ): ZIO[R, FailExceptionLike[?], E1] =
      body.exit.flatMap(runIntercept(None, _, true))

    /** Asserts that `ZIO[R, E, Any]` should fail with provided exception `E` and message `message`.
      * {{{
      *   testZ("effect should fail with message") {
      *     val effect = ZIO.fail(new IllegalArgumentException("BOOM!"))
      *     interceptFailureMessage[IllegalArgumentException]("BOOM!")(effect)
      *   }
      * }}}
      *
      * @param body
      *   the effect to be tested
      */
    def interceptFailureMessage[E1 <: E](
        message: String
    )(implicit loc: Location, ct: ClassTag[E1]): ZIO[R, FailExceptionLike[?], E1] =
      body.exit.flatMap(runIntercept(Some(message), _, false))
  }

  private def runIntercept[E <: Throwable](
      expectedExceptionMessage: Option[String],
      exit: Exit[Throwable, Any],
      shouldDie: Boolean
  )(implicit loc: Location, E: ClassTag[E]): IO[FailExceptionLike[?], E] =
    exit match {
      case Exit.Success(_)     =>
        ZIO
          .attempt {
            fail(
              s"expected exception of type '${E.runtimeClass.getName()}' but body evaluated successfully"
            )
          }
          .refineToOrDie[FailException]
      case Exit.Failure(cause) =>
        val e = if (shouldDie) cause.dieOption else cause.failureOption
        e match {
          case Some(error: FailExceptionLike[?])
              if !E.runtimeClass.isAssignableFrom(error.getClass()) =>
            ZIO.fail(error)

          case Some(error) if E.runtimeClass.isAssignableFrom(error.getClass()) =>
            if (expectedExceptionMessage.forall(_ == error.getMessage))
              ZIO.succeed[E](error.asInstanceOf[E])
            else
              ZIO.fail {
                val obtained = error.getClass().getName()
                new FailException(
                  s"intercept failed, exception '$obtained' had message '${error.getMessage}', which was different from expected message '${expectedExceptionMessage.get}'",
                  cause = error,
                  isStackTracesEnabled = false,
                  location = loc
                )
              }

          case Some(error) =>
            ZIO.fail {
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
            ZIO
              .attempt {
                fail(
                  s"expected exception of type '${E.runtimeClass.getName()}' but body evaluated successfully"
                )
              }
              .refineToOrDie[FailException]
        }
    }
}
