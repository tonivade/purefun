/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import com.github.tonivade.purefun.Eq;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Function2;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Instance;
import com.github.tonivade.purefun.Kind;
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

  static <T> Eq<Higher1<Sequence.µ, T>> eq(Eq<T> eqElement) {
    return (a, b) -> {
      Sequence<T> seq1 = Sequence.narrowK(a);
      Sequence<T> seq2 = Sequence.narrowK(b);
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

  static SemigroupK<Sequence.µ> semigroupK() {
    return SequenceSemigroupK.INSTANCE;
  }

  static MonoidK<Sequence.µ> monoidK() {
    return SequenceMonoidK.INSTANCE;
  }

  static Functor<Sequence.µ> functor() {
    return SequenceFunctor.INSTANCE;
  }

  static Applicative<Sequence.µ> applicative() {
    return SequenceApplicative.INSTANCE;
  }

  static SequenceMonad monad() {
    return SequenceMonad.INSTANCE;
  }

  static Alternative<Sequence.µ> alternative() {
    return SequenceAlternative.INSTANCE;
  }

  static Traverse<Sequence.µ> traverse() {
    return SequenceTraverse.INSTANCE;
  }

  static Foldable<Sequence.µ> foldable() {
    return SequenceFoldable.INSTANCE;
  }
}

@Instance
interface SequenceSemigroup<T> extends Semigroup<Sequence<T>> {

  SequenceSemigroup<?> INSTANCE = new SequenceSemigroup<Object>() { };

  @Override
  default Sequence<T> combine(Sequence<T> t1, Sequence<T> t2) {
    return t1.appendAll(t2);
  }
}

@Instance
interface SequenceMonoid<T> extends SequenceSemigroup<T>, Monoid<Sequence<T>> {

  SequenceMonoid<?> INSTANCE = new SequenceMonoid<Object>() { };

  @Override
  default Sequence<T> zero() {
    return ImmutableList.empty();
  }
}

@Instance
interface SequenceSemigroupK extends SemigroupK<Sequence.µ> {

  SequenceSemigroupK INSTANCE = new SequenceSemigroupK() { };

  @Override
  default <T> Higher1<Sequence.µ, T> combineK(Higher1<Sequence.µ, T> t1, Higher1<Sequence.µ, T> t2) {
    return Sequence.narrowK(t1).appendAll(Sequence.narrowK(t2)).kind1();
  }
}

@Instance
interface SequenceMonoidK extends MonoidK<Sequence.µ>, SequenceSemigroupK {

  SequenceMonoidK INSTANCE = new SequenceMonoidK() { };

  @Override
  default <T> Higher1<Sequence.µ, T> zero() {
    return ImmutableList.<T>empty().kind1();
  }
}

@Instance
interface SequenceFunctor extends Functor<Sequence.µ> {

  SequenceFunctor INSTANCE = new SequenceFunctor() { };

  @Override
  default <T, R> Higher1<Sequence.µ, R> map(Higher1<Sequence.µ, T> value, Function1<T, R> map) {
    return Sequence.narrowK(value).map(map).kind1();
  }
}

interface SequencePure extends Applicative<Sequence.µ> {

  @Override
  default <T> Higher1<Sequence.µ, T> pure(T value) {
    return ImmutableList.of(value).kind1();
  }
}

@Instance
interface SequenceApplicative extends SequencePure, Applicative<Sequence.µ> {

  SequenceApplicative INSTANCE = new SequenceApplicative() { };

  @Override
  default <T, R> Higher1<Sequence.µ, R> ap(Higher1<Sequence.µ, T> value, Higher1<Sequence.µ, Function1<T, R>> apply) {
    return Sequence.narrowK(apply).flatMap(map -> Sequence.narrowK(value).map(map)).kind1();
  }
}

@Instance
interface SequenceMonad extends SequencePure, Monad<Sequence.µ> {

  SequenceMonad INSTANCE = new SequenceMonad() { };

  @Override
  default <T, R> Higher1<Sequence.µ, R> flatMap(Higher1<Sequence.µ, T> value, Function1<T, ? extends Higher1<Sequence.µ, R>> map) {
    return Sequence.narrowK(value).flatMap(map.andThen(Sequence::narrowK)).kind1();
  }
}

@Instance
interface SequenceAlternative
    extends SequenceApplicative, SequenceMonoidK, Alternative<Sequence.µ> {
  SequenceAlternative INSTANCE = new SequenceAlternative() { };
}

@Instance
interface SequenceFoldable extends Foldable<Sequence.µ> {

  SequenceFoldable INSTANCE = new SequenceFoldable() { };

  @Override
  default <A, B> B foldLeft(Higher1<Sequence.µ, A> value, B initial, Function2<B, A, B> mapper) {
    return Sequence.narrowK(value).foldLeft(initial, mapper);
  }

  @Override
  default <A, B> Eval<B> foldRight(Higher1<Sequence.µ, A> value, Eval<B> initial, Function2<A, Eval<B>, Eval<B>> mapper) {
    return Sequence.narrowK(value).foldRight(initial, mapper);
  }
}

@Instance
interface SequenceTraverse extends Traverse<Sequence.µ>, SequenceFoldable {

  SequenceTraverse INSTANCE = new SequenceTraverse() { };

  @Override
  default <G extends Kind, T, R> Higher1<G, Higher1<Sequence.µ, R>> traverse(
      Applicative<G> applicative, Higher1<Sequence.µ, T> value,
      Function1<T, ? extends Higher1<G, R>> mapper) {
    return Sequence.narrowK(value).foldRight(
      applicative.pure(ImmutableList.<R>empty().kind1()),
      (a, acc) -> applicative.map2(mapper.apply(a), acc,
        (e, seq) -> Sequence.listOf(e).appendAll(Sequence.narrowK(seq)).kind1()));
  }
}
