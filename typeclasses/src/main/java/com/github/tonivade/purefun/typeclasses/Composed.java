/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.typeclasses.Nested.nest;
import static com.github.tonivade.purefun.typeclasses.Nested.unnest;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Function2;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Witness;
import com.github.tonivade.purefun.type.Eval;

interface ComposedFunctor<F extends Witness, G extends Witness> extends Functor<Nested<F, G>> {

  Functor<F> f();
  Functor<G> g();

  @Override
  default <T, R> Kind<Nested<F, G>, R> map(Kind<Nested<F, G>, T> value, Function1<T, R> map) {
    return nest(f().map(unnest(value), ga -> g().map(ga, map)));
  }
}

interface ComposedSemigroupK<F extends Witness, G extends Witness> extends SemigroupK<Nested<F, G>> {

  SemigroupK<F> f();

  @Override
  default <T> Kind<Nested<F, G>, T> combineK(Kind<Nested<F, G>, T> t1, Kind<Nested<F, G>, T> t2) {
    return nest(f().combineK(unnest(t1), unnest(t2)));
  }
}

interface ComposedMonoidK<F extends Witness, G extends Witness> extends MonoidK<Nested<F, G>>, ComposedSemigroupK<F, G> {

  @Override
  MonoidK<F> f();

  @Override
  default <T> Kind<Nested<F, G>, T> zero() {
    return nest(f().zero());
  }
}

interface ComposedApplicative<F extends Witness, G extends Witness> extends Applicative<Nested<F, G>> {

  Applicative<F> f();
  Applicative<G> g();

  @Override
  default <T> Kind<Nested<F, G>, T> pure(T value) {
    return nest(f().pure(g().pure(value)));
  }

  @Override
  default <T, R> Kind<Nested<F, G>, R> ap(Kind<Nested<F, G>, T> value,
      Kind<Nested<F, G>, Function1<T, R>> apply) {
    return nest(f().ap(unnest(value), f().map(unnest(apply), gfa -> ga -> g().ap(ga, gfa))));
  }
}

interface ComposedAlternative<F extends Witness, G extends Witness>
    extends ComposedApplicative<F, G>, ComposedMonoidK<F, G>, Alternative<Nested<F, G>> {

  @Override
  Alternative<F> f();
  @Override
  Alternative<G> g();
}

interface ComposedTraverse<F extends Witness, G extends Witness> extends Traverse<Nested<F, G>>, ComposedFoldable<F, G> {

  @Override
  Traverse<F> f();
  @Override
  Traverse<G> g();

  @Override
  default <H extends Witness, T, R> Kind<H, Kind<Nested<F, G>, R>> traverse(Applicative<H> applicative,
      Kind<Nested<F, G>, T> value, Function1<T, Kind<H, ? extends R>> mapper) {
    return applicative.map(
        f().traverse(applicative, unnest(value), ga -> g().traverse(applicative, ga, mapper)),
        Nested::nest);
  }
}

interface ComposedFoldable<F extends Witness, G extends Witness> extends Foldable<Nested<F, G>> {

  Foldable<F> f();
  Foldable<G> g();

  @Override
  default <A, B> B foldLeft(Kind<Nested<F, G>, A> value, B initial, Function2<B, A, B> mapper) {
    return f().foldLeft(unnest(value), initial, (a, b) -> g().foldLeft(b, a, mapper));
  }

  @Override
  default <A, B> Eval<B> foldRight(Kind<Nested<F, G>, A> value, Eval<B> initial, Function2<A, Eval<B>, Eval<B>> mapper) {
    return f().foldRight(unnest(value), initial, (a, lb) -> g().foldRight(a, lb, mapper));
  }
}

interface ComposedInvariant<F extends Witness, G extends Witness> extends Invariant<Nested<F, G>> {

  Invariant<F> f();
  Invariant<G> g();

  @Override
  default <A, B> Kind<Nested<F, G>, B> imap(Kind<Nested<F, G>, A> value,
                                               Function1<A, B> map,
                                               Function1<B, A> comap) {
    Function1<Kind<G, A>, Kind<G, B>> map2 = ga -> g().imap(ga, map, comap);
    Function1<Kind<G, B>, Kind<G, A>> comap2 = gb -> g().imap(gb, comap, map);
    return nest(f().imap(unnest(value), map2, comap2));
  }
}

interface ComposedInvariantCovariant<F extends Witness, G extends Witness> extends Invariant<Nested<F, G>> {

  Invariant<F> f();
  Functor<G> g();

  @Override
  default <A, B> Kind<Nested<F, G>, B> imap(Kind<Nested<F, G>, A> value,
                                               Function1<A, B> map,
                                               Function1<B, A> comap) {
    Function1<Kind<G, A>, Kind<G, B>> map2 = ga -> g().map(ga, map);
    Function1<Kind<G, B>, Kind<G, A>> comap2 = gb -> g().map(gb, comap);
    return nest(f().imap(unnest(value), map2, comap2));
  }
}

interface ComposedInvariantContravariant<F extends Witness, G extends Witness> extends Invariant<Nested<F, G>> {

  Invariant<F> f();
  Contravariant<G> g();

  @Override
  default <A, B> Kind<Nested<F, G>, B> imap(Kind<Nested<F, G>, A> value,
                                               Function1<A, B> map,
                                               Function1<B, A> comap) {
    Function1<Kind<G, A>, Kind<G, B>> map2 = ga -> g().contramap(ga, comap);
    Function1<Kind<G, B>, Kind<G, A>> comap2 = gb -> g().contramap(gb, map);
    return nest(f().imap(unnest(value), map2, comap2));
  }
}

interface ComposedCovariantContravariant<F extends Witness, G extends Witness> extends Contravariant<Nested<F, G>> {

  Functor<F> f();
  Contravariant<G> g();

  @Override
  default <A, B> Kind<Nested<F, G>, B> contramap(Kind<Nested<F, G>, A> value, Function1<B, A> map) {
    return nest(f().map(unnest(value), ga -> g().contramap(ga, map)));
  }
}

interface ComposedContravariant<F extends Witness, G extends Witness> extends Functor<Nested<F, G>> {

  Contravariant<F> f();
  Contravariant<G> g();

  @Override
  default <A, B> Kind<Nested<F, G>, B> map(Kind<Nested<F, G>, A> value, Function1<A, B> map) {
    return nest(f().contramap(unnest(value), gb -> g().contramap(gb, map)));
  }
}

interface ComposedContravariantCovariant<F extends Witness, G extends Witness> extends Contravariant<Nested<F, G>> {

  Contravariant<F> f();
  Functor<G> g();

  @Override
  default <A, B> Kind<Nested<F, G>, B> contramap(Kind<Nested<F, G>, A> value, Function1<B, A> map) {
    return nest(f().contramap(unnest(value), gb -> g().map(gb, map)));
  }
}