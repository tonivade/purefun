/*
 * Copyright (c) 2018-2022, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import static com.github.tonivade.purefun.data.SequenceOf.toSequence;

import com.github.tonivade.purefun.Eq;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Function2;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Witness;
import com.github.tonivade.purefun.data.ImmutableList;
import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.data.SequenceOf;
import com.github.tonivade.purefun.data.Sequence_;
import com.github.tonivade.purefun.type.Eval;
import com.github.tonivade.purefun.type.EvalOf;
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

  static <T> Eq<Kind<Sequence_, T>> eq(Eq<T> eqElement) {
    return (a, b) -> {
      Sequence<T> seq1 = SequenceOf.narrowK(a);
      Sequence<T> seq2 = SequenceOf.narrowK(b);
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
  default <T> Kind<Sequence_, T> combineK(Kind<Sequence_, ? extends T> t1, Kind<Sequence_, ? extends T> t2) {
    return SequenceOf.<T>narrowK(t1).appendAll(SequenceOf.narrowK(t2));
  }
}

interface SequenceMonoidK extends MonoidK<Sequence_>, SequenceSemigroupK {

  SequenceMonoidK INSTANCE = new SequenceMonoidK() {};

  @Override
  default <T> Kind<Sequence_, T> zero() {
    return ImmutableList.<T>empty();
  }
}

interface SequenceFunctor extends Functor<Sequence_> {

  SequenceFunctor INSTANCE = new SequenceFunctor() {};

  @Override
  default <T, R> Kind<Sequence_, R> map(Kind<Sequence_, ? extends T> value, Function1<? super T, ? extends R> map) {
    return SequenceOf.narrowK(value).map(map);
  }
}

interface SequencePure extends Applicative<Sequence_> {

  @Override
  default <T> Kind<Sequence_, T> pure(T value) {
    return ImmutableList.of(value);
  }
}

interface SequenceApplicative extends SequencePure, Applicative<Sequence_> {

  SequenceApplicative INSTANCE = new SequenceApplicative() {};

  @Override
  default <T, R> Kind<Sequence_, R> ap(Kind<Sequence_, ? extends T> value, 
      Kind<Sequence_, ? extends Function1<? super T, ? extends R>> apply) {
    return SequenceOf.narrowK(apply).flatMap(map -> SequenceOf.narrowK(value).map(map));
  }
}

interface SequenceMonad extends SequencePure, Monad<Sequence_> {

  SequenceMonad INSTANCE = new SequenceMonad() {};

  @Override
  default <T, R> Kind<Sequence_, R> flatMap(Kind<Sequence_, ? extends T> value, Function1<? super T, ? extends Kind<Sequence_, ? extends R>> map) {
    return SequenceOf.narrowK(value).flatMap(map.andThen(SequenceOf::narrowK));
  }
}

interface SequenceAlternative
    extends SequenceApplicative, SequenceMonoidK, Alternative<Sequence_> {

  SequenceAlternative INSTANCE = new SequenceAlternative() {};
}


interface SequenceFoldable extends Foldable<Sequence_> {

  SequenceFoldable INSTANCE = new SequenceFoldable() {};

  @Override
  default <A, B> B foldLeft(Kind<Sequence_, ? extends A> value, B initial, Function2<? super B, ? super A, ? extends B> mapper) {
    return SequenceOf.narrowK(value).foldLeft(initial, mapper);
  }

  @Override
  default <A, B> Eval<B> foldRight(Kind<Sequence_, ? extends A> value, Eval<? extends B> initial, 
      Function2<? super A, ? super Eval<? extends B>, ? extends Eval<? extends B>> mapper) {
    Eval<? extends B> foldRight = SequenceOf.narrowK(value).foldRight(initial, mapper);
    return EvalOf.narrowK(foldRight);
  }
}

interface SequenceTraverse extends Traverse<Sequence_>, SequenceFoldable {

  SequenceTraverse INSTANCE = new SequenceTraverse() {};

  @Override
  default <G extends Witness, T, R> Kind<G, Kind<Sequence_, R>> traverse(
      Applicative<G> applicative, Kind<Sequence_, T> value,
      Function1<? super T, ? extends Kind<G, ? extends R>> mapper) {
    return value.fix(toSequence()).foldLeft(
      applicative.pure(Sequence.<R>emptyList()),
      (acc, a) -> {
        Kind<G, ? extends R> apply = mapper.apply(a);
        return applicative.mapN(apply, acc, 
            (e, seq) -> seq.fix(toSequence()).append(e));
      });
  }
}
