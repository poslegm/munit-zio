package munit

import zio.Task

trait ZTests:
  self: FunSuite =>

  def testZIO(options: TestOptions)(body: Task[Any])(using loc: Location): Unit =
    test(options)(body)
