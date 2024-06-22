/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import com.github.tonivade.purefun.Kind;

import com.github.tonivade.purefun.core.Eq;
import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Function2;
import com.github.tonivade.purefun.data.ImmutableList;
import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.type.Eval;
import com.github.tonivade.purefun.typeclasses.Alternative;
import com.github.tonivade.purefun.typeclasses.Applicative;
import com.github.tonivade.purefun.typeclasses.Foldable;
import com.github.tonivade.purefun.typeclasses.Functor;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.Monoid;
import com.github.tonivade.purefun.typeclasses.MonoidK;
import com.github.tonivade.purefun.typeclasses.Semigroup;
import com.github.tonivade.purefun.typeclasses.SemigroupK;
import com.github.tonivade.purefun.typeclasses.Traverse;

@SuppressWarnings("unchecked")
public interface SequenceInstances {

  static <T> Eq<Kind<Sequence<?>, T>> eq(Eq<T> eqElement) {
    return (a, b) -> {
      Sequence<T> seq1 = a.fix();
      Sequence<T> seq2 = b.fix();
      return seq1.size() == seq2.size()
          && Sequence.zip(seq1, seq2).allMatch(tuple -> eqElement.eqv(tuple.get1(), tuple.get2()));
    };
  }

  static <T> Semigroup<Sequence<T>> semigroup() {
    return (SequenceSemigroup<T>) SequenceSemigroup.INSTANCE;
  }

  static <T> Monoid<Sequence<T>> monoid() {
    return (SequenceMonoid<T>) SequenceMonoid.INSTANCE;
  }

  static SemigroupK<Sequence<?>> semigroupK() {
    return SequenceSemigroupK.INSTANCE;
  }

  static MonoidK<Sequence<?>> monoidK() {
    return SequenceMonoidK.INSTANCE;
  }

  static Functor<Sequence<?>> functor() {
    return SequenceFunctor.INSTANCE;
  }

  static Applicative<Sequence<?>> applicative() {
    return SequenceApplicative.INSTANCE;
  }

  static Monad<Sequence<?>> monad() {
    return SequenceMonad.INSTANCE;
  }

  static Alternative<Sequence<?>> alternative() {
    return SequenceAlternative.INSTANCE;
  }

  static Traverse<Sequence<?>> traverse() {
    return SequenceTraverse.INSTANCE;
  }

  static Foldable<Sequence<?>> foldable() {
    return SequenceFoldable.INSTANCE;
  }
}

interface SequenceSemigroup<T> extends Semigroup<Sequence<T>> {

  SequenceSemigroup<?> INSTANCE = new SequenceSemigroup<>() {
  };

  @Override
  default Sequence<T> combine(Sequence<T> t1, Sequence<T> t2) {
    return t1.appendAll(t2);
  }
}

interface SequenceMonoid<T> extends SequenceSemigroup<T>, Monoid<Sequence<T>> {

  SequenceMonoid<?> INSTANCE = new SequenceMonoid<>() {
  };

  @Override
  default Sequence<T> zero() {
    return ImmutableList.empty();
  }
}

interface SequenceSemigroupK extends SemigroupK<Sequence<?>> {

  SequenceSemigroupK INSTANCE = new SequenceSemigroupK() {};

  @Override
  default <T> Kind<Sequence<?>, T> combineK(Kind<Sequence<?>, ? extends T> t1, Kind<Sequence<?>, ? extends T> t2) {
    return t1.<Sequence<T>>fix().appendAll(t2.fix());
  }
}

interface SequenceMonoidK extends MonoidK<Sequence<?>>, SequenceSemigroupK {

  SequenceMonoidK INSTANCE = new SequenceMonoidK() {};

  @Override
  default <T> Kind<Sequence<?>, T> zero() {
    return ImmutableList.empty();
  }
}

interface SequenceFunctor extends Functor<Sequence<?>> {

  SequenceFunctor INSTANCE = new SequenceFunctor() {};

  @Override
  default <T, R> Kind<Sequence<?>, R> map(Kind<Sequence<?>, ? extends T> value, Function1<? super T, ? extends R> map) {
    return value.<Sequence<T>>fix().map(map);
  }
}

interface SequencePure extends Applicative<Sequence<?>> {

  @Override
  default <T> Kind<Sequence<?>, T> pure(T value) {
    return ImmutableList.of(value);
  }
}

interface SequenceApplicative extends SequencePure, Applicative<Sequence<?>> {

  SequenceApplicative INSTANCE = new SequenceApplicative() {};

  @Override
  default <T, R> Kind<Sequence<?>, R> ap(Kind<Sequence<?>, ? extends T> value,
      Kind<Sequence<?>, ? extends Function1<? super T, ? extends R>> apply) {
    return apply.<Sequence<Function1<T, R>>>fix().flatMap(map -> value.<Sequence<T>>fix().map(map));
  }
}

interface SequenceMonad extends SequencePure, Monad<Sequence<?>> {

  SequenceMonad INSTANCE = new SequenceMonad() {};

  @Override
  default <T, R> Kind<Sequence<?>, R> flatMap(Kind<Sequence<?>, ? extends T> value, Function1<? super T, ? extends Kind<Sequence<?>, ? extends R>> map) {
    return value.<Sequence<T>>fix().flatMap(map);
  }
}

interface SequenceAlternative
    extends SequenceApplicative, SequenceMonoidK, Alternative<Sequence<?>> {

  SequenceAlternative INSTANCE = new SequenceAlternative() {};
}


interface SequenceFoldable extends Foldable<Sequence<?>> {

  SequenceFoldable INSTANCE = new SequenceFoldable() {};

  @Override
  default <A, B> B foldLeft(Kind<Sequence<?>, ? extends A> value, B initial, Function2<? super B, ? super A, ? extends B> mapper) {
    return value.<Sequence<A>>fix().foldLeft(initial, mapper);
  }

  @Override
  default <A, B> Eval<B> foldRight(Kind<Sequence<?>, ? extends A> value, Eval<? extends B> initial,
      Function2<? super A, ? super Eval<? extends B>, ? extends Eval<? extends B>> mapper) {
    Eval<? extends B> foldRight = value.<Sequence<A>>fix().foldRight(initial, mapper);
    return foldRight.fix();
  }
}

interface SequenceTraverse extends Traverse<Sequence<?>>, SequenceFoldable {

  SequenceTraverse INSTANCE = new SequenceTraverse() {};

  @Override
  default <G extends Kind<G, ?>, T, R> Kind<G, Kind<Sequence<?>, R>> traverse(
      Applicative<G> applicative, Kind<Sequence<?>, T> value,
      Function1<? super T, ? extends Kind<G, ? extends R>> mapper) {
    return value.<Sequence<T>>fix().foldLeft(
      applicative.pure(Sequence.emptyList()),
      (acc, a) -> {
        Kind<G, ? extends R> apply = mapper.apply(a);
        return applicative.mapN(apply, acc)
            .apply((e, seq) -> seq.<Sequence<R>>fix().append(e));
      });
  }
}
