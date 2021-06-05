package munit

import zio._

import scala.annotation.nowarn

abstract class ZIOSuite extends FunSuite {

  protected val runtime: Runtime[Any] = Runtime.global

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
}
