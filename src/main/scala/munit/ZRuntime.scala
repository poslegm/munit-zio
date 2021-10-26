package munit

import zio.{Runtime, IO, Exit}

import scala.concurrent.{Promise, Future}

trait ZRuntime {
  protected val runtime: Runtime[Any] =
    // suppress munit reports duplication
    Runtime.global.mapRuntimeConfig(_.copy(reportFatal = {
      case cause: FailExceptionLike[?] => throw cause
      case cause                       =>
        cause.printStackTrace
        throw cause
    }))

  /** Because original unsafeRunToFuture adds useless causes to `FailExceptionLike` and duplicates
    * errors on every test failure. See `cause.squashTraceWith(identity)`
    * https://github.com/zio/zio/blob/a53fb07d9bb78629e9564b8da92a824b9e2f6d09/core/shared/src/main/scala/zio/Runtime.scala#L130
    */
  private[munit] def unsafeRunToFuture[E, A](effect: IO[E, A]): Future[A] = {
    val promise = Promise[A]()
    val task    =
      effect.mapError {
        case t: Throwable => t
        case other        => new ZIOError(other)
      }
    runtime.unsafeRunAsyncWith(task) {
      case Exit.Success(res)   => promise.success(res)
      case Exit.Failure(cause) => promise.failure(cause.squash)
    }

    promise.future
  }

  private case class ZIOError(cause: Any) extends Exception(s"ZIO failed with ${cause.toString}")
}
