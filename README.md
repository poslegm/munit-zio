# munit-zio

Integration library between [MUnit](https://scalameta.org/munit) and
[ZIO](https://zio.dev).

## Usage

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.poslegm/munit-zio_3/badge.svg?kill_cache=1)](https://search.maven.org/artifact/com.github.poslegm/munit-zio_3/)

Library published for Scala 3, 2.13, 2.12.

### sbt

```scala
libraryDependencies += "org.scalameta" %% "munit" % munitVersion % Test
libraryDependencies += "com.github.poslegm" %% "munit-zio" % munitZIOVersion % Test
```

If you are using a version of sbt lower than 1.5.0, you will also need to add:
```scala
testFrameworks += new TestFramework("munit.Framework")
```

Run tests:

```
$ sbt
> test
> testOnly com.MySuite
```

Full MUnit documentation available [here](https://scalameta.org/munit/docs/getting-started.html).

<!-- TOC depthFrom:2 depthTo:3 -->

- [ZSuite](#zsuite)
- [Assertions](#assertions)
  - [assertZ](#assertz)
  - [assertNoDiffZ](#assertnodiffz)
  - [assertEqualsZ (assertNotEqualsZ)](#assertequalsz-assertnotequalsz)
- [Failure checking](#failure-checking)
  - [interceptFailure](#interceptfailure)
  - [interceptFailureMessage](#interceptfailuremessage)
  - [interceptDefect](#interceptdefect)
- [Resource management](#resource-management)
  - [Test-local fixture](#test-local-fixture)
  - [Suite-local fixture](#suite-local-fixture)
- [ZLayers providing](#zlayers-providing)

<!-- /TOC -->

### ZSuite

MUnit integration provided by abstract class `ZSuite`. It contains `testZ`
function which runs `ZIO` effect as MUnit test.
> Note: `ZSuite` will throw `WrongTestMethodError` if you pass `ZIO` into plain
> MUnit `test` method. It prevents non-running effects in tests. Also compiler
> option `-Wvalue-discard` is highly recommended for working with effects.

```scala
import munit.*
import zio.*

class SimpleZIOSpec extends ZSuite:
  testZ("1 + 1 = 2") {
    for
      a <- ZIO(1)
      b <- ZIO(1)
    yield assertEquals(a + b, 2)
  }
```

### Assertions

You can use any of MUnit assertions in `ZIO` code, but `ZSuite` contains
helpful `ZIO`-specific assertions.

#### assertZ

Asserts that `ZIO[R, E, Boolean]` returns `true`.

```scala
testZ("false OR true should be true") {
  val effect = ZIO.succeed(false || true)
  assertZ(effect)
}
```
 
#### assertNoDiffZ

Asserts that `ZIO[R, E, String]` has no difference with expected string. Pretty
prints diff unlike just `assertEqualsZ`.

```scala
testZ("strings are the same") {
  val effect = ZIO.succeed("string")
  assertNoDiffZ(effect, "string")
}
``` 

#### assertEqualsZ (assertNotEqualsZ)

Asserts that `ZIO[R, E, A]` returns the same result as expected
 
```scala
testZ("values are the same") {
  val effect = ZIO.succeed(42)
  assertEqualsZ(effect, 42)
}
```

### Failure checking

Extension methods for intercept `ZIO`'s failures or defects.

#### interceptFailure

Asserts that `ZIO[R, E, Any]` should fail with provided exception `E`.

```scala
testZ("effect should fail") {
  val effect = ZIO.fail(new IllegalArgumentException("BOOM!"))
  effect.interceptFailure[IllegalArgumentException]
}
```

#### interceptFailureMessage

Asserts that `ZIO[R, E, Any]` should fail with provided exception `E` and
message `message`.

```scala
testZ("effect should fail with message") {
  val effect = ZIO.fail(new IllegalArgumentException("BOOM!"))
  effect.interceptFailureMessage[IllegalArgumentException]("BOOM!")
}
```
 
#### interceptDefect

Asserts that `ZIO[R, E, Any]` should die with provided exception `E`.

```scala
testZ("effect should die") {
  val effect = ZIO.die(new IllegalArgumentException("BOOM!"))
  effect.interceptDefect[IllegalArgumentException]
}
```

### Resource management

Resource management in ZSuite based on `ZManaged` and MUnit fixtures.
"Test-local" means that resource will be acquired and released on __every__
`testZ` execution.  "Suite-local" means that resource will be acquired __before
all__ `testZ` executions and released __after all__ `testZ` executions.
Resources from test- and suite-local fixtures can be accessed directly from
`testZ`.

#### Test-local fixture

You can create test-local fixture from `ZManaged` or raw acquire/release effects.

```scala
// define fixture with raw acquire/release effects
// `options` contains metadata about current test like its name
val rawZIOFunFixture = ZTestLocalFixture(options => ZIO.succeed(s"acquired ${options.name}")) {
  str => putStrLn(s"cleanup [$str]").provideLayer(Console.live)
}

// use it with `testZ` extension method with resource access
rawZIOFunFixture.testZ("allocate resource with ZIO FunFixture") { str => // <- resource
  val effect = ZIO(str.trim)
  assertNoDiffZ(effect, "acquired allocate resource with ZIO FunFixture")
}

// similarly for `ZManaged`
val ZManagedFunFixture = ZTestLocalFixture { options =>
  ZManaged.make(ZIO.succeed(s"acquired ${options.name} with ZManaged")) { str =>
    putStrLn(s"cleanup [$str] with ZManaged").provideLayer(Console.live).orDie
  }
}

ZManagedFunFixture.testZ("allocate resource with ZManaged FunFixture") { str =>
  val effect = ZIO(str.trim)
  assertNoDiffZ(effect, "acquired allocate resource with ZManaged FunFixture with ZManaged")
}
```

#### Suite-local fixture

Suite-local fixture can be created from `ZManaged` and provides synchronous
access to its resource.

```scala
// dirty example, don't do it in real code
var state   = 0
val fixture = ZSuiteLocalFixture(
  "sample",
  ZManaged.make(ZIO.effectTotal { state += 1; state })(_ => ZIO.effectTotal { state += 1 })
)

// suite-local fixture should be necessarily initialized
override val munitFixtures = Seq(fixture)

test("suite local fixture works") {
  val current: Int = fixture() // <- access resource
  assertEquals(current, 1)
}
```

### ZLayers providing

You can provide dependencies to your tests with test-local fixture. There is
`testZLayered` extension method for fixtures with `ULayer[R]` resource. It just
provides layers into test body, so `ZIO[R, E, A]` can be converted to `ZIO[Any,
E, A]` with `ULayer[R]` fixture.

```scala
// accessor methods dependent on some `StatefulRepository` and `Service`:

def clean: URIO[Has[StatefulRepository], Unit] = ZIO.service[StatefulRepository].flatMap(_.clean)
def fetch: RIO[Has[Service], Unit] = ZIO.service[Service].flatMap(_.fetch)
def write: RIO[Has[Service], Unit] = ZIO.service[Service].flatMap(_.write)

// ===========

val layersFixture = ZTestLocalFixture { _ =>
  // wire layers in `ZManaged`'s acquire
  ZManaged.make(ZIO.succeed(StatefulRepository.test >+> Service.test))(layers =>
    // graceful release resources after test execution
    clean.provideLayer(layers)
  )
}

layersFixture.testZLayered("auto provide layer") {
  val effect: RIO[Has[Service], Unit] = write *> write *> fetch
  assertEqualsZ(effect, 2) // Has[Service] will be provided from fixture
}
```

## Inspired by
[munit-cats-effect](https://github.com/typelevel/munit-cats-effect).
