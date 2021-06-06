package munit

import zio.*

import java.util.concurrent.ExecutionException

abstract class ZSuite
    extends FunSuite
    with ZAssertions
    with ZFixtures
    with ZFixtureSyntax
    with ZRuntime:

  /** Runs test returning `ZIO[Any, E, Any]`
    *
    * {{{
    * testZ("simple effect test") {
    *   val effect = for
    *     a <- ZIO(1)
    *     b <- ZIO(2)
    *   yield a + b
    *
    *   assertZ(effect, 3)
    * }
    *
    * }}}
    *
    * @param name
    *   test name
    * @param body
    *   test body
    */
  def testZ[E](name: String)(body: IO[E, Any])(using Location): Unit =
    testZ(TestOptions(name))(body)

  def testZ[E](options: TestOptions)(body: IO[E, Any])(using Location): Unit =
    test(options)(unsafeRunToFuture(body))

  override def munitValueTransforms: List[ValueTransform] =
    super.munitValueTransforms ::: List(munitZIOTransform)

  class WrongTestMethodError
      extends Exception(
        "ZIO value passed into munit `test` function. This is wrong, because the code will not be executed. Use `testZ` for ZIO effects."
      )
  private val munitZIOTransform: ValueTransform =
    new ValueTransform(
      "ZIO",
      { case z: ZIO[?, ?, ?] => throw WrongTestMethodError() }
    )
