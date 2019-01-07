/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.data;

import static com.github.tonivade.purefun.Function1.identity;
import static java.util.Objects.requireNonNull;
import static java.util.Spliterators.spliteratorUnknownSize;
import static java.util.stream.Collectors.groupingBy;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.github.tonivade.purefun.Filterable;
import com.github.tonivade.purefun.FlatMap1;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Function2;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Matcher1;
import com.github.tonivade.purefun.Operator2;
import com.github.tonivade.purefun.PartialFunction1;
import com.github.tonivade.purefun.Tuple;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.type.Eval;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.typeclasses.Alternative;
import com.github.tonivade.purefun.typeclasses.Applicative;
import com.github.tonivade.purefun.typeclasses.Eq;
import com.github.tonivade.purefun.typeclasses.Foldable;
import com.github.tonivade.purefun.typeclasses.Monoid;
import com.github.tonivade.purefun.typeclasses.MonoidK;
import com.github.tonivade.purefun.typeclasses.Semigroup;
import com.github.tonivade.purefun.typeclasses.SemigroupK;
import com.github.tonivade.purefun.typeclasses.Traverse;

public interface Sequence<E> extends Iterable<E>, FlatMap1<Sequence.µ, E>, Filterable<E> {

  final class µ implements Kind {}

  int size();

  boolean contains(E element);
  Sequence<E> append(E element);
  Sequence<E> remove(E element);
  Sequence<E> appendAll(Sequence<E> other);
  Sequence<E> removeAll(Sequence<E> other);

  Sequence<E> reverse();

  @Override
  <R> Sequence<R> map(Function1<E, R> mapper);

  @Override
  <R> Sequence<R> flatMap(Function1<E, ? extends Higher1<Sequence.µ, R>> mapper);

  @SuppressWarnings("unchecked")
  default <V> Sequence<V> flatten() {
    try {
      return ((Sequence<Sequence<V>>) this).flatMap(identity());
    } catch (ClassCastException e) {
      throw new UnsupportedOperationException("cannot be flattened");
    }
  }

  @Override
  Sequence<E> filter(Matcher1<E> matcher);

  default Option<E> reduce(Operator2<E> operator) {
    return Option.from(stream().reduce(operator::apply));
  }

  default E fold(E initial, Operator2<E> operator) {
    return stream().reduce(initial, operator::apply);
  }

  default <U> U foldLeft(U initial, Function2<U, E, U> combinator) {
    U accumulator = initial;
    for (E element : this) {
      accumulator = combinator.apply(accumulator, element);
    }
    return accumulator;
  }

  default <U> U foldRight(U initial, Function2<E, U, U> combinator) {
    return reverse().foldLeft(initial, (acc, e) -> combinator.apply(e, acc));
  }

  default <R> Sequence<R> collect(PartialFunction1<E, R> function) {
    return filter(function::isDefinedAt).map(function::apply);
  }

  default <G> ImmutableMap<G, ImmutableList<E>> groupBy(Function1<E, G> selector) {
    return ImmutableMap.from(stream().collect(groupingBy(selector::apply))).mapValues(ImmutableList::from);
  }

  default ImmutableList<E> asList() {
    return ImmutableList.from(stream());
  }

  default ImmutableArray<E> asArray() {
    return ImmutableArray.from(stream());
  }

  default ImmutableSet<E> asSet() {
    return ImmutableSet.from(stream());
  }

  default ImmutableTree<E> asTree() {
    return ImmutableTree.from(stream());
  }

  default Stream<E> stream() {
    return StreamSupport.stream(spliterator(), false);
  }

  default boolean isEmpty() {
    return size() == 0;
  }

  default Stream<Tuple2<Integer, E>> zipWithIndex() {
    return zip(Stream.iterate(0, i -> i + 1), this.stream());
  }

  @SafeVarargs
  static <E> ImmutableArray<E> arrayOf(E... elements) {
    return ImmutableArray.of(elements);
  }

  @SafeVarargs
  static <E> ImmutableList<E> listOf(E... elements) {
    return ImmutableList.of(elements);
  }

  @SafeVarargs
  static <E> ImmutableSet<E> setOf(E... elements) {
    return ImmutableSet.of(elements);
  }

  @SafeVarargs
  static <E extends Comparable<E>> ImmutableTree<E> treeOf(E... elements) {
    return ImmutableTree.of(elements);
  }

  static <A, B> Stream<Tuple2<A, B>> zip(Iterator<A> first, Iterator<B> second) {
    return asStream(new PairIterator<>(first, second));
  }

  static <A, B> Stream<Tuple2<A, B>> zip(Stream<A> first, Stream<B> second) {
    return zip(first.iterator(), second.iterator());
  }

  static <A, B> Stream<Tuple2<A, B>> zip(Sequence<A> first, Sequence<B> second) {
    return zip(first.stream(), second.stream());
  }

  static <E> Stream<E> asStream(Iterator<E> iterator) {
    return StreamSupport.stream(spliteratorUnknownSize(iterator, Spliterator.ORDERED), false);
  }

  static <T> Sequence<T> narrowK(Higher1<Sequence.µ, T> hkt) {
    return (Sequence<T>) hkt;
  }

  static <T> Eq<Higher1<Sequence.µ, T>> eq(Eq<T> eqElement) {
    return (a, b) -> {
      Sequence<T> seq1 = narrowK(a);
      Sequence<T> seq2 = narrowK(b);
      return seq1.size() == seq2.size()
          && zip(seq1, seq2).allMatch(tuple -> eqElement.eqv(tuple.get1(), tuple.get2()));
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

final class PairIterator<A, B> implements Iterator<Tuple2<A, B>> {

  private final Iterator<A> first;
  private final Iterator<B> second;

  PairIterator(Iterator<A> first, Iterator<B> second) {
    this.first = requireNonNull(first);
    this.second = requireNonNull(second);
  }

  @Override
  public boolean hasNext() {
    return first.hasNext() && second.hasNext();
  }

  @Override
  public Tuple2<A, B> next() {
    return Tuple.of(first.next(), second.next());
  }
}

interface SequenceSemigroup<T> extends Semigroup<Sequence<T>> {

  @Override
  default Sequence<T> combine(Sequence<T> t1, Sequence<T> t2) {
    return t1.appendAll(t2);
  }
}

interface SequenceMonoid<T> extends SequenceSemigroup<T>, Monoid<Sequence<T>> {

  @Override
  default Sequence<T> zero() {
    return ImmutableList.empty();
  }
}

interface SequenceSemigroupK extends SemigroupK<Sequence.µ> {

  @Override
  default <T> Sequence<T> combineK(Higher1<Sequence.µ, T> t1, Higher1<Sequence.µ, T> t2) {
    return Sequence.narrowK(t1).appendAll(Sequence.narrowK(t2));
  }
}

interface SequenceMonoidK extends MonoidK<Sequence.µ>, SequenceSemigroupK {

  @Override
  default <T> Sequence<T> zero() {
    return ImmutableList.empty();
  }
}

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

interface SequenceAlternative extends SequenceApplicative, SequenceMonoidK, Alternative<Sequence.µ> {}

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

interface SequenceTraverse extends Traverse<Sequence.µ> {

  @Override
  default <G extends Kind, T, R> Higher1<G, Higher1<Sequence.µ, R>> traverse(
      Applicative<G> applicative, Higher1<Sequence.µ, T> value,
      Function1<T, ? extends Higher1<G, R>> mapper) {
    return Sequence.narrowK(value).foldRight(applicative.pure(ImmutableList.empty()),
        (a, acc) -> applicative.map2(mapper.apply(a), acc,
            (e, seq) -> Sequence.listOf(e).appendAll(Sequence.narrowK(seq))));
  }
}