package arrow.optics

import arrow.core.*
import arrow.core.extensions.monoid
import arrow.core.extensions.either.applicative.applicative
import arrow.core.extensions.option.monoid.monoid
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
class OptionTest : UnitSpec() {

  init {

    testLaws(PrismLaws.laws(
      prism = Option.some(),
      aGen = Gen.option(Gen.int()),
      bGen = Gen.int(),
      funcGen = Gen.functionAToB(Gen.int()),
      EQA = Eq.any(),
      EQOptionB = Eq.any()
    ))

    testLaws(PrismLaws.laws(
      prism = Option.none(),
      aGen = Gen.option(Gen.int()),
      bGen = Gen.create { Unit },
      funcGen = Gen.functionAToB(Gen.create { Unit }),
      EQA = Eq.any(),
      EQOptionB = Eq.any()
    ))

    testLaws(IsoLaws.laws(
      iso = Option.toNullable<Int>().reverse(),
      aGen = Gen.int().orNull(),
      bGen = Gen.option(Gen.int()),
      EQA = Eq.any(),
      EQB = Eq.any(),
      funcGen = Gen.functionAToB(Gen.option(Gen.int())),
      bMonoid = Option.monoid(Int.monoid())
    ))

    testLaws(IsoLaws.laws(
      iso = Option.toEither(),
      aGen = Gen.option(Gen.int()),
      bGen = Gen.either(Gen.create { Unit }, Gen.int()),
      funcGen = Gen.functionAToB(Gen.either(Gen.create { Unit }, Gen.int())),
      EQA = Eq.any(),
      EQB = Eq.any(),
      bMonoid = object : Monoid<Either<Unit, Int>> {
        override fun Either<Unit, Int>.combine(b: Either<Unit, Int>): Either<Unit, Int> =
          Either.applicative<Unit>().run { this@combine.map2(b) { (a, b) -> a + b }.fix() }

        override fun empty(): Either<Unit, Int> = Right(0)
      }
    ))

  }
}
