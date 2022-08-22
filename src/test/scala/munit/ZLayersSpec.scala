package munit

import zio.*

import SampleDependencies.*

class ZLayerSpec extends ZSuite {
  testZ("simple layers providing") {
    val deps = ZLayer.succeed(new A()) >>> ZLayer.fromFunction(new B(_))

    val effect = ZIO.service[B].map(_.g)
    assertZ(effect.provideLayer(deps))
  }

  val layersFixture = ZTestLocalFixture { _ =>
    ZIO.acquireRelease(ZIO.succeed(StatefulRepository.test >+> Service.test))(layers =>
      clean.provideLayer(layers)
    )
  }

  layersFixture.testZ("fixture layers providing") { layers =>
    val effect = write *> write *> fetch
    assertEqualsZ(effect.provideLayer(layers), 2)
  }

  layersFixture.testZ("fixture layers providing after clean") { layers =>
    val effect = write *> write *> fetch
    assertEqualsZ(effect.provideLayer(layers), 2)
  }

  layersFixture.testZLayered("auto provide layer") {
    val effect = write *> write *> fetch
    assertEqualsZ(effect, 2)
  }
}

object SampleDependencies {
  // stateless
  class A {
    def f: Boolean = false
  }

  class B(a: A) {
    def g: Boolean = !a.f
  }
  // ======

  // stateful
  class StatefulRepository {
    private var state = 0

    def getState: Task[Int] = ZIO.attempt { state }

    def inc: Task[Unit] =
      ZIO.attempt { state += 1 }

    def clean: UIO[Unit] =
      ZIO.attempt { state = 0 }.orDie
  }

  object StatefulRepository {
    val test = ZLayer.succeed(new StatefulRepository())
  }

  class Service(repo: StatefulRepository) {
    def fetch: Task[Int]  = repo.getState
    def write: Task[Unit] = repo.inc
  }

  object Service {
    val test = ZLayer.fromFunction(new Service(_))
  }

  def clean = ZIO.service[StatefulRepository].flatMap(_.clean)
  def fetch = ZIO.service[Service].flatMap(_.fetch)
  def write = ZIO.service[Service].flatMap(_.write)
}
