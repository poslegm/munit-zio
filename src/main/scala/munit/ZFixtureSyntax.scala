package munit

import zio.{RIO, ULayer, IO}

trait ZFixtureSyntax {
  self: ZSuite =>

  implicit class FixtureSyntax[T](private val fixture: FunFixture[T]) {
    def testZ[E](name: String)(body: T => IO[E, Any])(implicit loc: Location): Unit =
      fixture.testZ(TestOptions(name))(body)

    def testZ[E](options: TestOptions)(body: T => IO[E, Any])(implicit loc: Location): Unit =
      fixture.test(options)(arg => unsafeRunToFuture(body(arg)))
  }

  implicit class LayerFixtureSyntax[R](private val fixture: FunFixture[ULayer[R]]) {
    def testZLayered(name: String)(body: RIO[R, Any])(implicit loc: Location): Unit =
      fixture.testZLayered(TestOptions(name))(body)

    def testZLayered(options: TestOptions)(body: RIO[R, Any])(implicit loc: Location): Unit =
      fixture.testZ(options)(layer => body.provideLayer(layer))
  }
}
