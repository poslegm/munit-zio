package munit

import zio.{Runtime, IO, Exit}

import scala.concurrent.{Promise, Future}

trait ZRuntime:
  protected val runtime: Runtime[Any] = Runtime.global.withReportFailure { cause =>
    cause.dieOption.foreach {
      // suppress munit reports duplication
      case _: FailExceptionLike[?] =>
      case other                   => System.err.println(cause.prettyPrint)
    }
  }

  /** Because original unsafeRunToFuture adds useless causes to `FailExceptionLike` and duplicates
    * errors on every test failure. See `cause.squashTraceWith(identity)`
    * https://github.com/zio/zio/blob/a53fb07d9bb78629e9564b8da92a824b9e2f6d09/core/shared/src/main/scala/zio/Runtime.scala#L130
    */
  private[munit] def unsafeRunToFuture[E, A](effect: IO[E, A]): Future[A] =
    val promise = Promise[A]()
    val task    =
      effect.mapError {
        case t: Throwable => t
        case other        => ZIOError(other)
      }
    runtime.unsafeRunAsync(task) {
      case Exit.Success(res)   => promise.success(res)
      case Exit.Failure(cause) => promise.failure(cause.squash)
    }

    promise.future

  private final case class ZIOError(cause: Any)
      extends Exception(s"ZIO failed with ${cause.toString}")
