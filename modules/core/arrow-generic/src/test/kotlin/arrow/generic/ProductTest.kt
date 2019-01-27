package arrow.generic

import arrow.core.*
import arrow.core.extensions.`try`.applicative.applicative
import arrow.core.extensions.option.applicative.applicative
import arrow.core.extensions.option.monoid.monoid
import arrow.product
import arrow.test.UnitSpec
import arrow.test.generators.genNonEmptyList
import arrow.test.generators.genOption
import arrow.test.generators.genTuple
import arrow.test.laws.EqLaws
import arrow.test.laws.MonoidLaws
import arrow.test.laws.SemigroupLaws
import arrow.typeclasses.Applicative
import io.kotlintest.properties.Gen
import io.kotlintest.properties.forAll
import io.kotlintest.runner.junit4.KotlinTestRunner
import io.kotlintest.shouldBe
import org.junit.runner.RunWith

@product
data class Person(val name: String, val age: Int, val related: Option<Person>) {
  companion object
}

fun genPerson(): Gen<Person> {
  val genRelated =
    Gen.bind(Gen.string(), Gen.int()) { name: String, age: Int -> Person(name, age, None) }
  return Gen.bind(
    Gen.string(),
    Gen.int(),
    genOption(genRelated)
  ) { name: String, age: Int, related: Option<Person> -> Person(name, age, related) }
}

fun tuple3PersonGen(): Gen<Tuple3<String, Int, Option<Person>>> =
  genTuple(Gen.string(), Gen.int(), genOption(genPerson()))

inline fun <reified F> Applicative<F>.testPersonApplicative() {
  forAll(Gen.string(), Gen.int(), genPerson()) { a, b, c ->
    mapToPerson(just(a), just(b), just(c.some())) == just(Person(a, b, c.some()))
  }
}

@RunWith(KotlinTestRunner::class)
class ProductTest : UnitSpec() {

  init {

    ".tupled()" {
      forAll(genPerson()) {
        it.tupled() == Tuple3(it.name, it.age, it.related)
      }
    }

    ".toPerson()" {
      forAll(tuple3PersonGen()) {
        it.toPerson() == Person(it.a, it.b, it.c)
      }
    }

    ".tupledLabeled()" {
      forAll(genPerson()) {
        it.tupledLabeled() == Tuple3(
          "name" toT it.name,
          "age" toT it.age,
          "related" toT it.related
        )
      }
    }

    "List<@product>.combineAll()" {
      forAll(genNonEmptyList(genPerson()).map { it.all }) {
        it.combineAll() == it.reduce { a, b -> a + b }
      }
    }

    "Applicative Syntax" {
      Option.applicative().testPersonApplicative()
      Try.applicative().testPersonApplicative()
    }

    "Show instance defaults to .toString()" {
      with(Person.show()) {
        forAll(genPerson()) {
          it.show() == it.toString()
        }
      }
    }

    "Eq instance defaults to .equals()" {
      with(Person.eq()) {
        forAll(genPerson(), genPerson()) { a, b ->
          a.eqv(b) == (a == b)
        }
      }
    }

    "Semigroup combine" {
      forAll(genPerson(), genPerson()) { a, b ->
        with(Person.semigroup()) {
          a.combine(b) == Person(
            a.name + b.name,
            a.age + b.age,
            Option.monoid(this).combineAll(listOf(a.related, b.related))
          )
        }
      }
    }

    "Semigroup + syntax" {
      forAll(genPerson(), genPerson()) { a, b ->
        a + b == Person(
          a.name + b.name,
          a.age + b.age,
          Option.monoid(Person.monoid()).combineAll(listOf(a.related, b.related))
        )
      }
    }

    "Monoid empty" {
      Person.monoid().empty() shouldBe Person("", 0, None)
    }

    "Monoid empty syntax" {
      emptyPerson() shouldBe Person("", 0, None)
    }

    val getPersonWithAge: (Int) -> Person = { age: Int -> Person("", age, None)}

    testLaws(
      EqLaws.laws(Person.eq(), getPersonWithAge),
      SemigroupLaws.laws(Person.semigroup(), getPersonWithAge(1), getPersonWithAge(2), getPersonWithAge(3), Person.eq()),
      MonoidLaws.laws(Person.monoid(), genPerson(), Person.eq())
    )
  }
}
