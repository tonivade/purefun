/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import com.github.tonivade.purefun.Eq;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Function2;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.data.ImmutableList;
import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.data.Sequence_;
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

  static <T> Eq<Higher1<Sequence_, T>> eq(Eq<T> eqElement) {
    return (a, b) -> {
      Sequence<T> seq1 = Sequence_.narrowK(a);
      Sequence<T> seq2 = Sequence_.narrowK(b);
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

  static SemigroupK<Sequence_> semigroupK() {
    return SequenceSemigroupK.INSTANCE;
  }

  static MonoidK<Sequence_> monoidK() {
    return SequenceMonoidK.INSTANCE;
  }

  static Functor<Sequence_> functor() {
    return SequenceFunctor.INSTANCE;
  }

  static Applicative<Sequence_> applicative() {
    return SequenceApplicative.INSTANCE;
  }

  static SequenceMonad monad() {
    return SequenceMonad.INSTANCE;
  }

  static Alternative<Sequence_> alternative() {
    return SequenceAlternative.INSTANCE;
  }

  static Traverse<Sequence_> traverse() {
    return SequenceTraverse.INSTANCE;
  }

  static Foldable<Sequence_> foldable() {
    return SequenceFoldable.INSTANCE;
  }
}

interface SequenceSemigroup<T> extends Semigroup<Sequence<T>> {

  SequenceSemigroup<?> INSTANCE = new SequenceSemigroup<Object>() {};

  @Override
  default Sequence<T> combine(Sequence<T> t1, Sequence<T> t2) {
    return t1.appendAll(t2);
  }
}

interface SequenceMonoid<T> extends SequenceSemigroup<T>, Monoid<Sequence<T>> {

  SequenceMonoid<?> INSTANCE = new SequenceMonoid<Object>() {};

  @Override
  default Sequence<T> zero() {
    return ImmutableList.empty();
  }
}

interface SequenceSemigroupK extends SemigroupK<Sequence_> {

  SequenceSemigroupK INSTANCE = new SequenceSemigroupK() {};

  @Override
  default <T> Higher1<Sequence_, T> combineK(Higher1<Sequence_, T> t1, Higher1<Sequence_, T> t2) {
    return Sequence_.narrowK(t1).appendAll(Sequence_.narrowK(t2));
  }
}

interface SequenceMonoidK extends MonoidK<Sequence_>, SequenceSemigroupK {

  SequenceMonoidK INSTANCE = new SequenceMonoidK() {};

  @Override
  default <T> Higher1<Sequence_, T> zero() {
    return ImmutableList.<T>empty();
  }
}

interface SequenceFunctor extends Functor<Sequence_> {

  SequenceFunctor INSTANCE = new SequenceFunctor() {};

  @Override
  default <T, R> Higher1<Sequence_, R> map(Higher1<Sequence_, T> value, Function1<T, R> map) {
    return Sequence_.narrowK(value).map(map);
  }
}

interface SequencePure extends Applicative<Sequence_> {

  @Override
  default <T> Higher1<Sequence_, T> pure(T value) {
    return ImmutableList.of(value);
  }
}

interface SequenceApplicative extends SequencePure, Applicative<Sequence_> {

  SequenceApplicative INSTANCE = new SequenceApplicative() {};

  @Override
  default <T, R> Higher1<Sequence_, R> ap(Higher1<Sequence_, T> value, Higher1<Sequence_, Function1<T, R>> apply) {
    return Sequence_.narrowK(apply).flatMap(map -> Sequence_.narrowK(value).map(map));
  }
}

interface SequenceMonad extends SequencePure, Monad<Sequence_> {

  SequenceMonad INSTANCE = new SequenceMonad() {};

  @Override
  default <T, R> Higher1<Sequence_, R> flatMap(Higher1<Sequence_, T> value, Function1<T, ? extends Higher1<Sequence_, R>> map) {
    return Sequence_.narrowK(value).flatMap(map.andThen(Sequence_::narrowK));
  }
}

interface SequenceAlternative
    extends SequenceApplicative, SequenceMonoidK, Alternative<Sequence_> {

  SequenceAlternative INSTANCE = new SequenceAlternative() {};
}


interface SequenceFoldable extends Foldable<Sequence_> {

  SequenceFoldable INSTANCE = new SequenceFoldable() {};

  @Override
  default <A, B> B foldLeft(Higher1<Sequence_, A> value, B initial, Function2<B, A, B> mapper) {
    return Sequence_.narrowK(value).foldLeft(initial, mapper);
  }

  @Override
  default <A, B> Eval<B> foldRight(Higher1<Sequence_, A> value, Eval<B> initial, Function2<A, Eval<B>, Eval<B>> mapper) {
    return Sequence_.narrowK(value).foldRight(initial, mapper);
  }
}

interface SequenceTraverse extends Traverse<Sequence_>, SequenceFoldable {

  SequenceTraverse INSTANCE = new SequenceTraverse() {};

  @Override
  default <G extends Kind, T, R> Higher1<G, Higher1<Sequence_, R>> traverse(
      Applicative<G> applicative, Higher1<Sequence_, T> value,
      Function1<T, ? extends Higher1<G, R>> mapper) {
    return Sequence_.narrowK(value).foldRight(
      applicative.pure(ImmutableList.<R>empty()),
      (a, acc) -> applicative.map2(mapper.apply(a), acc,
        (e, seq) -> Sequence.listOf(e).appendAll(Sequence_.narrowK(seq))));
  }
}
