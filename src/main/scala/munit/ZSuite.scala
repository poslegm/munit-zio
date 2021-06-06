package munit

import zio.*

import scala.concurrent.Future

abstract class ZSuite extends FunSuite with ZAssertions with ZFixtures with ZFixtureSyntax:

  protected val runtime: Runtime[Any] = Runtime.global.withReportFailure { cause =>
    cause.dieOption.foreach {
      // suppress munit reports duplication
      case _: FailExceptionLike[?] =>
      case other                   => System.err.println(cause.prettyPrint)
    }
  }

  protected def unsafeRunToFuture[E, A](effect: IO[E, A]): Future[A] =
    runtime.unsafeRunToFuture(effect.mapError {
      case t: Throwable => t
      case other        => ZIOError(other)
    })

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

  def testZ[E](name: String)(body: IO[E, Any])(using Location): Unit =
    testZ(TestOptions(name))(body)

  def testZ[E](options: TestOptions)(body: IO[E, Any])(using Location): Unit =
    test(options)(unsafeRunToFuture(body))

  private final case class ZIOError(cause: Any)
      extends Exception(s"ZIO failed with ${cause.toString}")
