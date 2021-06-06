# munit-zio

Integration library between [MUnit](https://scalameta.org/munit) and
[ZIO](https://zio.dev).

## Usage

```scala
import munit.*
import zio.*

class SimpleZIOSpec extends ZSuite:
  testZ("1 + 1 = 2") {
    val effect = for
      a <- ZIO(1)
      b <- ZIO(1)
    yield a + b

    assertEqualsZ(a + b, 2)
  }
```

## TODO

- Scala Steward
- Examples in README
- Publish to Maven Central

Inspired by
[munit-cats-effect](https://github.com/typelevel/munit-cats-effect).
