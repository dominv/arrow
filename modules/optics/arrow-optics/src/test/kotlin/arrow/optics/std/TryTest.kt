package arrow.optics

import arrow.core.Either
import arrow.core.Right
import arrow.core.Try
import arrow.core.fix
import arrow.data.Invalid
import arrow.data.Valid
import arrow.data.Validated
import arrow.core.extensions.either.applicative.applicative
import arrow.test.UnitSpec
import arrow.test.generators.*
import arrow.test.laws.IsoLaws
import arrow.test.laws.PrismLaws
import arrow.typeclasses.Eq
import arrow.typeclasses.Monoid
import io.kotlintest.runner.junit4.KotlinTestRunner
import io.kotlintest.properties.Gen
import org.junit.runner.RunWith

@RunWith(KotlinTestRunner::class)
class TryTest : UnitSpec() {
  init {

    testLaws(PrismLaws.laws(
      prism = Try.success(),
      aGen = genTry(Gen.int()),
      bGen = Gen.int(),
      funcGen = genFunctionAToB(Gen.int()),
      EQA = Eq.any(),
      EQOptionB = Eq.any()
    ))

    testLaws(PrismLaws.laws(
      prism = Try.failure(),
      aGen = genTry(Gen.int()),
      bGen = genThrowable(),
      funcGen = genFunctionAToB(genThrowable()),
      EQA = Eq.any(),
      EQOptionB = Eq.any()
    ))

    testLaws(IsoLaws.laws(
      iso = Try.toEither(),
      aGen = genTry(Gen.int()),
      bGen = genEither(genThrowable(), Gen.int()),
      funcGen = genFunctionAToB(genEither(genThrowable(), Gen.int())),
      EQA = Eq.any(),
      EQB = Eq.any(),
      bMonoid = object : Monoid<Either<Throwable, Int>> {
        override fun Either<Throwable, Int>.combine(b: Either<Throwable, Int>): Either<Throwable, Int> =
          Either.applicative<Throwable>().run { this@combine.map2(b) { (a, b) -> a + b }.fix() }

        override fun empty(): Either<Throwable, Int> = Right(0)
      }
    ))

    testLaws(IsoLaws.laws(
      iso = Try.toValidated(),
      aGen = genTry(Gen.int()),
      bGen = genValidated(genThrowable(), Gen.int()),
      funcGen = genFunctionAToB(genValidated(genThrowable(), Gen.int())),
      EQA = Eq.any(),
      EQB = Eq.any(),
      bMonoid = object : Monoid<Validated<Throwable, Int>> {
        override fun Validated<Throwable, Int>.combine(b: Validated<Throwable, Int>): Validated<Throwable, Int> =
          when (this) {
            is Invalid -> {
              when (b) {
                is Invalid -> Invalid(e)
                is Valid -> b
              }
            }
            is Valid -> {
              when (b) {
                is Invalid -> b
                is Valid -> Valid(a + b.a)
              }
            }
          }

        override fun empty() = Valid(0)
      }
    ))

  }
}
