package arrow.core.extensions

import arrow.Kind
import arrow.core.Either
import arrow.core.Eval
import arrow.core.ForListK
import arrow.core.ListK
import arrow.core.ListKOf
import arrow.core.Option
import arrow.core.SequenceK
import arrow.core.Tuple2
import arrow.core.extensions.listk.monad.monad
import arrow.core.extensions.listk.semigroup.plus
import arrow.core.fix
import arrow.core.k
import arrow.extension
import arrow.typeclasses.Alternative
import arrow.typeclasses.Applicative
import arrow.typeclasses.Apply
import arrow.typeclasses.Eq
import arrow.typeclasses.Foldable
import arrow.typeclasses.Functor
import arrow.typeclasses.FunctorFilter
import arrow.typeclasses.Hash
import arrow.typeclasses.Monad
import arrow.typeclasses.MonadCombine
import arrow.typeclasses.MonadFilter
import arrow.typeclasses.MonadSyntax
import arrow.typeclasses.Monoid
import arrow.typeclasses.MonoidK
import arrow.typeclasses.Monoidal
import arrow.typeclasses.Semigroup
import arrow.typeclasses.SemigroupK
import arrow.typeclasses.Semigroupal
import arrow.typeclasses.Show
import arrow.typeclasses.Traverse
import arrow.core.combineK as listCombineK
import kotlin.collections.plus as listPlus

@extension
interface ListKSemigroup<A> : Semigroup<ListK<A>> {
  override fun ListK<A>.combine(b: ListK<A>): ListK<A> =
    (this.listPlus(b)).k()
}

@extension
interface ListKMonoid<A> : Monoid<ListK<A>>, ListKSemigroup<A> {
  override fun empty(): ListK<A> =
    emptyList<A>().k()
}

@extension
interface ListKEq<A> : Eq<ListKOf<A>> {

  fun EQ(): Eq<A>

  override fun ListKOf<A>.eqv(b: ListKOf<A>): Boolean =
    if (fix().size == b.fix().size) fix().zip(b.fix()) { aa, bb ->
      EQ().run { aa.eqv(bb) }
    }.fold(true) { acc, bool ->
      acc && bool
    }
    else false
}

@extension
interface ListKShow<A> : Show<ListKOf<A>> {
  override fun ListKOf<A>.show(): String =
    toString()
}

@extension
interface ListKFunctor : Functor<ForListK> {
  override fun <A, B> Kind<ForListK, A>.map(f: (A) -> B): ListK<B> =
    fix().map(f)
}

@extension
interface ListKApply : Apply<ForListK> {
  override fun <A, B> Kind<ForListK, A>.ap(ff: Kind<ForListK, (A) -> B>): ListK<B> =
    fix().ap(ff)

  override fun <A, B> Kind<ForListK, A>.map(f: (A) -> B): ListK<B> =
    fix().map(f)

  override fun <A, B, Z> Kind<ForListK, A>.map2(fb: Kind<ForListK, B>, f: (Tuple2<A, B>) -> Z): ListK<Z> =
    fix().map2(fb, f)
}

@extension
interface ListKApplicative : Applicative<ForListK> {
  override fun <A, B> Kind<ForListK, A>.ap(ff: Kind<ForListK, (A) -> B>): ListK<B> =
    fix().ap(ff)

  override fun <A, B> Kind<ForListK, A>.map(f: (A) -> B): ListK<B> =
    fix().map(f)

  override fun <A, B, Z> Kind<ForListK, A>.map2(fb: Kind<ForListK, B>, f: (Tuple2<A, B>) -> Z): ListK<Z> =
    fix().map2(fb, f)

  override fun <A> just(a: A): ListK<A> =
    ListK.just(a)
}

@extension
interface ListKMonad : Monad<ForListK> {
  override fun <A, B> Kind<ForListK, A>.ap(ff: Kind<ForListK, (A) -> B>): ListK<B> =
    fix().ap(ff)

  override fun <A, B> Kind<ForListK, A>.flatMap(f: (A) -> Kind<ForListK, B>): ListK<B> =
    fix().flatMap(f)

  override fun <A, B> tailRecM(a: A, f: kotlin.Function1<A, ListKOf<Either<A, B>>>): ListK<B> =
    ListK.tailRecM(a, f)

  override fun <A, B> Kind<ForListK, A>.map(f: (A) -> B): ListK<B> =
    fix().map(f)

  override fun <A, B, Z> Kind<ForListK, A>.map2(fb: Kind<ForListK, B>, f: (Tuple2<A, B>) -> Z): ListK<Z> =
    fix().map2(fb, f)

  override fun <A> just(a: A): ListK<A> =
    ListK.just(a)
}

@extension
interface ListKFoldable : Foldable<ForListK> {
  override fun <A, B> Kind<ForListK, A>.foldLeft(b: B, f: (B, A) -> B): B =
    fix().foldLeft(b, f)

  override fun <A, B> Kind<ForListK, A>.foldRight(lb: Eval<B>, f: (A, Eval<B>) -> Eval<B>): Eval<B> =
    fix().foldRight(lb, f)

  override fun <A> Kind<ForListK, A>.isEmpty(): kotlin.Boolean =
    fix().isEmpty()
}

@extension
interface ListKTraverse : Traverse<ForListK> {
  override fun <A, B> Kind<ForListK, A>.map(f: (A) -> B): ListK<B> =
    fix().map(f)

  override fun <G, A, B> Kind<ForListK, A>.traverse(AP: Applicative<G>, f: (A) -> Kind<G, B>): Kind<G, ListK<B>> =
    fix().traverse(AP, f)

  override fun <A, B> Kind<ForListK, A>.foldLeft(b: B, f: (B, A) -> B): B =
    fix().foldLeft(b, f)

  override fun <A, B> Kind<ForListK, A>.foldRight(lb: Eval<B>, f: (A, Eval<B>) -> Eval<B>): Eval<B> =
    fix().foldRight(lb, f)

  override fun <A> Kind<ForListK, A>.isEmpty(): Boolean =
    fix().isEmpty()
}

@extension
interface ListKSemigroupK : SemigroupK<ForListK> {
  override fun <A> Kind<ForListK, A>.combineK(y: Kind<ForListK, A>): ListK<A> =
    fix().listCombineK(y)
}

@extension
interface ListKSemigroupal : Semigroupal<ForListK> {
  override fun <A, B> Kind<ForListK, A>.product(fb: Kind<ForListK, B>): Kind<ForListK, Tuple2<A, B>> =
    fb.fix().ap(fix().map { a: A -> { b: B -> Tuple2(a, b) } })
}

@extension
interface ListKMonoidal : Monoidal<ForListK>, ListKSemigroupal {
  override fun <A> identity(): Kind<ForListK, A> = ListK.empty()
}

@extension
interface ListKMonoidK : MonoidK<ForListK> {
  override fun <A> empty(): ListK<A> =
    ListK.empty()

  override fun <A> Kind<ForListK, A>.combineK(y: Kind<ForListK, A>): ListK<A> =
    fix().listCombineK(y)
}

@extension
interface ListKHash<A> : Hash<ListKOf<A>>, ListKEq<A> {

  fun HA(): Hash<A>

  override fun EQ(): Eq<A> = HA()

  override fun ListKOf<A>.hash(): Int = fix().foldLeft(1) { hash, a ->
    31 * hash + HA().run { a.hash() }
  }
}

@extension
interface ListKFunctorFilter : FunctorFilter<ForListK> {
  override fun <A, B> Kind<ForListK, A>.filterMap(f: (A) -> Option<B>): ListK<B> =
    fix().filterMap(f)

  override fun <A, B> Kind<ForListK, A>.map(f: (A) -> B): ListK<B> =
    fix().map(f)
}

fun <A> ListK.Companion.fx(c: suspend MonadSyntax<ForListK>.() -> A): ListK<A> =
  ListK.monad().fx.monad(c).fix()

@extension
interface ListKMonadCombine : MonadCombine<ForListK>, ListKAlternative {
  override fun <A> empty(): ListK<A> =
    ListK.empty()

  override fun <A, B> Kind<ForListK, A>.filterMap(f: (A) -> Option<B>): ListK<B> =
    fix().filterMap(f)

  override fun <A, B> Kind<ForListK, A>.ap(ff: Kind<ForListK, (A) -> B>): ListK<B> =
    fix().ap(ff)

  override fun <A, B> Kind<ForListK, A>.flatMap(f: (A) -> Kind<ForListK, B>): ListK<B> =
    fix().flatMap(f)

  override fun <A, B> tailRecM(a: A, f: kotlin.Function1<A, ListKOf<Either<A, B>>>): ListK<B> =
    ListK.tailRecM(a, f)

  override fun <A, B> Kind<ForListK, A>.map(f: (A) -> B): ListK<B> =
    fix().map(f)

  override fun <A, B, Z> Kind<ForListK, A>.map2(fb: Kind<ForListK, B>, f: (Tuple2<A, B>) -> Z): ListK<Z> =
    fix().map2(fb, f)

  override fun <A> just(a: A): ListK<A> =
    ListK.just(a)

  override fun <A> Kind<ForListK, A>.some(): ListK<SequenceK<A>> =
    if (this.fix().isEmpty()) ListK.empty()
    else map { Sequence { object : Iterator<A> {
      override fun hasNext(): Boolean = true

      override fun next(): A = it
    } }.k() }.k()

  override fun <A> Kind<ForListK, A>.many(): ListK<SequenceK<A>> =
    if (this.fix().isEmpty()) listOf(emptySequence<A>().k()).k()
    else map { Sequence { object : Iterator<A> {
      override fun hasNext(): Boolean = true

      override fun next(): A = it
    } }.k() }.k()
}

@extension
interface ListKMonadFilter : MonadFilter<ForListK> {
  override fun <A> empty(): ListK<A> =
    ListK.empty()

  override fun <A, B> Kind<ForListK, A>.filterMap(f: (A) -> Option<B>): ListK<B> =
    fix().filterMap(f)

  override fun <A, B> Kind<ForListK, A>.ap(ff: Kind<ForListK, (A) -> B>): ListK<B> =
    fix().ap(ff)

  override fun <A, B> Kind<ForListK, A>.flatMap(f: (A) -> Kind<ForListK, B>): ListK<B> =
    fix().flatMap(f)

  override fun <A, B> tailRecM(a: A, f: kotlin.Function1<A, ListKOf<Either<A, B>>>): ListK<B> =
    ListK.tailRecM(a, f)

  override fun <A, B> Kind<ForListK, A>.map(f: (A) -> B): ListK<B> =
    fix().map(f)

  override fun <A, B, Z> Kind<ForListK, A>.map2(fb: Kind<ForListK, B>, f: (Tuple2<A, B>) -> Z): ListK<Z> =
    fix().map2(fb, f)

  override fun <A> just(a: A): ListK<A> =
    ListK.just(a)
}

@extension
interface ListKAlternative : Alternative<ForListK>, ListKApplicative {
  override fun <A> empty(): Kind<ForListK, A> = emptyList<A>().k()
  override fun <A> Kind<ForListK, A>.orElse(b: Kind<ForListK, A>): Kind<ForListK, A> =
    (this.fix() + b.fix()).k()
}
