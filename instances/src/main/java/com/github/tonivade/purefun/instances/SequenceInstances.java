/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
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
import com.github.tonivade.purefun.typeclasses.Monoid;
import com.github.tonivade.purefun.typeclasses.MonoidK;
import com.github.tonivade.purefun.typeclasses.Semigroup;
import com.github.tonivade.purefun.typeclasses.SemigroupK;
import com.github.tonivade.purefun.typeclasses.Traverse;

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
    return new SequenceSemigroup<T>() {};
  }

  static <T> Monoid<Sequence<T>> monoid() {
    return new SequenceMonoid<T>() {};
  }

  static SemigroupK<Sequence.µ> semigroupK() {
    return new SequenceSemigroupK() {};
  }

  static MonoidK<Sequence.µ> monoidK() {
    return new SequenceMonoidK() {};
  }

  static Alternative<Sequence.µ> alternative() {
    return new SequenceAlternative() {};
  }

  static Traverse<Sequence.µ> traverse() {
    return new SequenceTraverse() {};
  }

  static Foldable<Sequence.µ> foldable() {
    return new SequenceFoldable() {};
  }
}

@Instance
interface SequenceSemigroup<T> extends Semigroup<Sequence<T>> {

  @Override
  default Sequence<T> combine(Sequence<T> t1, Sequence<T> t2) {
    return t1.appendAll(t2);
  }
}

@Instance
interface SequenceMonoid<T> extends SequenceSemigroup<T>, Monoid<Sequence<T>> {

  @Override
  default Sequence<T> zero() {
    return ImmutableList.empty();
  }
}

@Instance
interface SequenceSemigroupK extends SemigroupK<Sequence.µ> {

  @Override
  default <T> Sequence<T> combineK(Higher1<Sequence.µ, T> t1, Higher1<Sequence.µ, T> t2) {
    return Sequence.narrowK(t1).appendAll(Sequence.narrowK(t2));
  }
}

@Instance
interface SequenceMonoidK extends MonoidK<Sequence.µ>, SequenceSemigroupK {

  @Override
  default <T> Sequence<T> zero() {
    return ImmutableList.empty();
  }
}

@Instance
interface SequenceApplicative extends Applicative<Sequence.µ> {

  @Override
  default <T> Sequence<T> pure(T value) {
    return ImmutableList.of(value);
  }

  @Override
  default <T, R> Sequence<R> ap(Higher1<Sequence.µ, T> value, Higher1<Sequence.µ, Function1<T, R>> apply) {
    return Sequence.narrowK(apply).flatMap(map -> Sequence.narrowK(value).map(map));
  }
}

@Instance
interface SequenceAlternative extends SequenceApplicative, SequenceMonoidK, Alternative<Sequence.µ> {}

@Instance
interface SequenceFoldable extends Foldable<Sequence.µ> {

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

  @Override
  default <G extends Kind, T, R> Higher1<G, Higher1<Sequence.µ, R>> traverse(
      Applicative<G> applicative, Higher1<Sequence.µ, T> value,
      Function1<T, ? extends Higher1<G, R>> mapper) {
    return Sequence.narrowK(value).foldRight(applicative.pure(ImmutableList.empty()),
        (a, acc) -> applicative.map2(mapper.apply(a), acc,
            (e, seq) -> Sequence.listOf(e).appendAll(Sequence.narrowK(seq))));
  }
}
