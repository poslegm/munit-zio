package munit

import zio.*

abstract class ZSuite extends FunSuite with ZTests with ZAssertions with ZFixtures:

  protected val runtime: Runtime[Any] = Runtime.global.withReportFailure { cause =>
    cause.dieOption.foreach {
      // suppress munit reports duplication
      case _: FailExceptionLike[?] =>
      case other                   => System.err.println(cause.prettyPrint)
    }
  }

  override def munitValueTransforms: List[ValueTransform] =
    super.munitValueTransforms ::: List(munitZIOTransform)

  private val munitZIOTransform: ValueTransform =
    new ValueTransform(
      "ZIO",
      { case z: ZIO[?, ?, ?] =>
        runtime.unsafeRunToFuture[Throwable, Any](
          z.mapError {
            case e: Throwable => e
            case other        => ZIOError(other)
          }.asInstanceOf[ZIO[Any, Throwable, Any]]
        )
      }
    )

  private final case class ZIOError(cause: Any)
      extends Exception(s"ZIO failed with ${cause.toString}")
