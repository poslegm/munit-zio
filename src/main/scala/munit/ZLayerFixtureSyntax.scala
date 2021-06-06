package munit

import zio.{RIO, ULayer, IO}

trait ZFixtureSyntax:
  self: ZSuite =>

  extension [T](fixture: FunFixture[T])
    def testZ[E](name: String)(body: T => IO[E, Any])(using Location): Unit =
      fixture.testZ(TestOptions(name))(body)

    def testZ[E](options: TestOptions)(body: T => IO[E, Any])(using Location): Unit =
      fixture.test(options)(arg => runtime.unsafeRun(body(arg)))

  extension [R](fixture: FunFixture[ULayer[R]])
    def testZ(name: String)(body: RIO[R, Any])(using Location): Unit =
      fixture.testZ(TestOptions(name))(body)

    def testZ(options: TestOptions)(body: RIO[R, Any])(using Location): Unit =
      fixture.testZ(options)(layer => body.provideLayer(layer))
