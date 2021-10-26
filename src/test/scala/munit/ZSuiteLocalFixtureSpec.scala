package munit

import zio.*

class ZSuiteLocalFixtureSpec extends ZSuite {
  var state   = 0
  val fixture = ZSuiteLocalFixture(
    "sample",
    ZManaged.acquireReleaseWith(ZIO.succeed { state += 1; state })(_ => ZIO.succeed { state += 1 })
  )

  override val munitFixtures = Seq(fixture)

  override def beforeAll(): Unit =
    assertEquals(state, 0)

  test("suite local fixture works") {
    assertEquals(fixture(), 1)
  }

  override def afterAll(): Unit =
    assertEquals(state, 2)
}
