package munit

import zio.*

class ZSuiteLocalFixtureSpec extends ZSuite {
  var state   = 0
  val fixture = ZSuiteLocalFixture(
    "sample",
    ZIO.acquireRelease(ZIO.attempt { state += 1; state })(_ => ZIO.attempt { state += 1 }.orDie)
  )

  override val munitFixtures: Seq[Fixture[_]] = Seq(fixture)

  override def beforeAll(): Unit =
    assertEquals(state, 0)

  test("suite local fixture works") {
    assertEquals(fixture(), 1)
  }

  override def afterAll(): Unit =
    assertEquals(state, 2)

  test("FixtureNotInstantiatedException is thrown when fixture not in munitFixtures") {
    val unregistered = ZSuiteLocalFixture("unregistered", ZIO.succeed(42))
    intercept[ZSuiteLocalFixture.FixtureNotInstantiatedException] {
      unregistered()
    }
  }
}
