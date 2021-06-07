package munit

import zio._

class ZSuiteSpec extends ZSuite {
  testZ("ZIO success on standart test") {
    for {
      x   <- ZIO(41)
      res <- ZIO(x + 1)
    } yield assertEquals(res, 42)
  }

  testZ("ZIO fail on standart test".fail) {
    for {
      x   <- ZIO(41)
      res <- ZIO(x + 1)
    } yield assertEquals(res, 43)
  }
}
