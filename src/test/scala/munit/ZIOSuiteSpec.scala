package munit

import zio._

class ZIOSuiteSpec extends ZIOSuite {
  test("ZIO success") {
    for {
      x   <- ZIO(41)
      res <- ZIO(x + 1)
    } yield assertEquals(res, 42)
  }

  test("ZIO fail".fail) {
    for {
      x   <- ZIO(41)
      res <- ZIO(x + 1)
    } yield assertEquals(res, 43)
  }
}
