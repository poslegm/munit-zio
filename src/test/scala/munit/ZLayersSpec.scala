package munit

import zio.*

import SampleDependencies.*

class ZLayerSpec extends ZSuite:
  testZ("simple layers providing") {
    val deps = ZLayer.succeed(A()) >>> ZLayer.fromService[A, B](B(_))

    val effect = ZIO.service[B].map(_.g)
    assertZ(effect.provideLayer(deps))
  }

  val layersFixture = ZTestLocalFixture { _ =>
    ZManaged.make(ZIO.succeed(StatefulRepository.test >+> Service.test))(layers =>
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

  layersFixture.testZ("auto provide layer") {
    val effect = write *> write *> fetch
    assertEqualsZ(effect, 3)
  }

object SampleDependencies:
  // stateless
  class A:
    def f: Boolean = false

  class B(a: A):
    export a.*
    def g: Boolean = !f
  // ======

  // stateful
  class StatefulRepository:
    private var state = 0

    def getState: Task[Int] = ZIO.effectTotal { state }

    def inc: Task[Unit] =
      ZIO.effectTotal { state += 1 }

    def clean: UIO[Unit] =
      ZIO.effectTotal { state = 0 }

  object StatefulRepository:
    val test = ZLayer.succeed(StatefulRepository())

  class Service(repo: StatefulRepository):
    def fetch: Task[Int]  = repo.getState
    def write: Task[Unit] = repo.inc

  object Service:
    val test = ZLayer.fromService[StatefulRepository, Service](Service(_))

  def clean = ZIO.service[StatefulRepository].flatMap(_.clean)
  def fetch = ZIO.service[Service].flatMap(_.fetch)
  def write = ZIO.service[Service].flatMap(_.write)
