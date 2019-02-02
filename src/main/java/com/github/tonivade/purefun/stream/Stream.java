/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.stream;

import static com.github.tonivade.purefun.Nothing.nothing;
import static com.github.tonivade.purefun.data.Sequence.asStream;
import static java.util.Objects.requireNonNull;

import java.util.Arrays;

import com.github.tonivade.purefun.Filterable;
import com.github.tonivade.purefun.FlatMap2;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Function2;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Higher2;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Matcher1;
import com.github.tonivade.purefun.Nothing;
import com.github.tonivade.purefun.Operator1;
import com.github.tonivade.purefun.PartialFunction1;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.data.ImmutableList;
import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.monad.IO;
import com.github.tonivade.purefun.type.Id;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.typeclasses.Defer;
import com.github.tonivade.purefun.typeclasses.Monad;

public interface Stream<F extends Kind, T> extends FlatMap2<Stream.µ, F, T>, Filterable<T> {

  final class µ implements Kind {}

  Stream<F, T> head();
  Stream<F, T> tail();

  Stream<F, T> concat(Stream<F, T> other);
  Stream<F, T> append(Higher1<F, T> other);
  Stream<F, T> prepend(Higher1<F, T> other);

  Stream<F, T> take(int n);
  Stream<F, T> drop(int n);

  Higher1<F, Option<Cons<F, T>>> extract();

  @Override
  Stream<F, T> filter(Matcher1<T> matcher);
  Stream<F, T> takeWhile(Matcher1<T> matcher);
  Stream<F, T> dropWhile(Matcher1<T> matcher);

  <R> Stream<F, R> collect(PartialFunction1<T, R> partial);
  <R> Higher1<F, R> foldLeft(R begin, Function2<R, T, R> combinator);
  <R> Higher1<F, R> foldRight(Higher1<F, R> begin, Function2<T, Higher1<F, R>, Higher1<F, R>> combinator);

  @Override
  <R> Stream<F, R> map(Function1<T, R> map);
  @Override
  <R> Stream<F, R> flatMap(Function1<T, ? extends Higher2<Stream.µ, F, R>> map);
  <R> Stream<F, R> mapEval(Function1<T, Higher1<F, R>> mapper);

  Stream<F, T> repeat();
  Stream<F, T> intersperse(Higher1<F, T> value);

  Higher1<F, Boolean> exists(Matcher1<T> matcher);
  Higher1<F, Boolean> forall(Matcher1<T> matcher);

  Higher1<F, Option<Tuple2<T, Stream<F, T>>>> split();

  default <G extends Kind, R> Stream<G, R> through(Function1<Stream<F, T>, Stream<G, R>> function) {
    return function.apply(this);
  }

  default Higher1<F, Sequence<T>> asSequence() {
    return foldLeft(ImmutableList.empty(), (acc, a) -> acc.append(a));
  }

  default Higher1<F, String> asString() {
    return foldLeft("", (acc, a) -> acc + a);
  }

  default Higher1<F, Nothing> drain() {
    return foldLeft(nothing(), (acc, a) -> acc);
  }

  default <R> Stream<F, R> andThen(Higher1<F, R> next) {
    return mapEval(ignore -> next);
  }

  static StreamOf<IO.µ> ofIO() {
    return of(IO.monad(), IO.defer());
  }

  static StreamOf<Id.µ> ofId() {
    return of(Id.monad(), Id.defer());
  }

  static <F extends Kind> StreamOf<F> of(Monad<F> monad, Defer<F> defer) {
    return new StreamOf<F>() {

      @Override
      public Monad<F> monad() { return monad; }

      @Override
      public Defer<F> defer() { return defer; }
    };
  }

  static <F extends Kind, T> Stream<F, T> narrowK(Higher1<Higher1<Stream.µ, F>, T> hkt) {
    return (Stream<F, T>) hkt;
  }

  static <F extends Kind, T> Stream<F, T> narrowK(Higher2<Stream.µ, F, T> hkt) {
    return (Stream<F, T>) hkt;
  }

  interface StreamOf<F extends Kind> {

    Monad<F> monad();
    Defer<F> defer();

    default <T> Stream<F, T> empty() {
      return new Nil<>(monad(), defer());
    }

    @SuppressWarnings("unchecked")
    default <T> Stream<F, T> of(T... values) {
      return from(Arrays.stream(values));
    }

    default <T> Stream<F, T> pure(T value) {
      return eval(monad().pure(value));
    }

    default <T> Stream<F, T> suspend(Producer<Stream<F, T>> lazy) {
      return new Suspend<>(monad(), defer(), defer().defer(lazy.andThen(monad()::pure)));
    }

    default <T> Stream<F, T> eval(Higher1<F, T> value) {
      return new Cons<>(monad(), defer(), value, empty());
    }

    default <T> Stream<F, T> from(Iterable<T> iterable) {
      return from(asStream(iterable.iterator()));
    }

    default <T> Stream<F, T> from(java.util.stream.Stream<T> stream) {
      return from(ImmutableList.from(stream));
    }

    default <T> Stream<F, T> from(Sequence<T> sequence) {
      return sequence.foldLeft(empty(), (acc, a) -> acc.append(monad().pure(a)));
    }

    default <T> Stream<F, T> iterate(T seed, Operator1<T> generator) {
      return new Cons<>(monad(), defer(), monad().pure(seed),
          suspend(() -> iterate(generator.apply(seed), generator)));
    }

    default <T> Stream<F, T> iterate(Producer<T> generator) {
      return new Cons<>(monad(), defer(), monad().pure(generator.get()),
          suspend(() -> iterate(generator)));
    }

    default <A, B, R> Stream<F, R> zipWith(Stream<F, A> s1, Stream<F, B> s2, Function2<A, B, R> combinator) {
      return new Suspend<>(monad(), defer(), defer().defer(
        () -> monad().map2(s1.extract(), s2.extract(),
          (op1, op2) -> {
            Higher1<Option.µ, Stream<F, R>> result = Option.monad().map2(op1, op2,
              (cons1, cons2) -> {
                Higher1<F, R> head = monad().map2(cons1.head, cons2.head, combinator);
                Stream<F, R> tail = zipWith(cons1.tail, cons2.tail, combinator);
                return new Cons<>(monad(), defer(), head, tail);
              });
            return Option.narrowK(result).getOrElse(this::empty);
          })
        ));
    }

    default <A, B> Stream<F, Tuple2<A, B>> zip(Stream<F, A> s1, Stream<F, B> s2) {
      return zipWith(s1, s2, Tuple2::of);
    }

    default <A> Stream<F, Tuple2<A, Integer>> zipWithIndex(Stream<F, A> stream) {
      return zip(stream, iterate(0, x -> x + 1));
    }

    default <A> Stream<F, A> merge(Stream<F, A> s1, Stream<F, A> s2) {
      return new Suspend<>(monad(), defer(), defer().defer(
        () -> monad().map2(s1.split(), s2.split(),
          (opt1, opt2) -> {
            Higher1<Option.µ, Stream<F, A>> result = Option.monad().map2(opt1, opt2,
              (t1, t2) -> {
                Higher1<F, A> head = monad().pure(t1.get1());
                Stream<F, A> tail = pure(t2.get1()).concat(merge(t1.get2(), t2.get2()));
                return new Cons<>(monad(), defer(), head, tail);
              });
            return Option.narrowK(result).getOrElse(this::empty);
          })
        ));
    }
  }
}

final class Cons<F extends Kind, T> implements Stream<F, T> {

  private final Monad<F> monad;
  private final Defer<F> defer;
  final Higher1<F, T> head;
  final Stream<F, T> tail;

  Cons(Monad<F> monad, Defer<F> defer, Higher1<F, T> head, Stream<F, T> tail) {
    this.monad = requireNonNull(monad);
    this.defer = requireNonNull(defer);
    this.head = requireNonNull(head);
    this.tail = requireNonNull(tail);
  }

  @Override
  public Stream<F, T> head() {
    return take(1);
  }

  @Override
  public Stream<F, T> tail() {
    return tail;
  }

  @Override
  public Higher1<F, Option<Cons<F, T>>> extract() {
    return monad.pure(Option.some(this));
  }

  @Override
  public Stream<F, T> concat(Stream<F, T> other) {
    return suspend(() -> cons(head, tail.concat(other)));
  }

  @Override
  public Stream<F, T> append(Higher1<F, T> other) {
    return suspend(() -> cons(head, tail.append(other)));
  }

  @Override
  public Stream<F, T> prepend(Higher1<F, T> other) {
    return suspend(() -> cons(other, tail.prepend(head)));
  }

  @Override
  public Stream<F, T> take(int n) {
    return n > 0 ? suspend(() -> cons(head, tail.take(n - 1))) : empty();
  }

  @Override
  public Stream<F, T> drop(int n) {
    return n > 0 ? suspend(() -> tail.drop(n - 1)) : this;
  }

  @Override
  public Stream<F, T> takeWhile(Matcher1<T> matcher) {
    return suspendF(() -> monad.map(head,
        t -> matcher.match(t) ? cons(head, tail.takeWhile(matcher)) : empty()));
  }

  @Override
  public Stream<F, T> dropWhile(Matcher1<T> matcher) {
    return suspendF(() ->
            monad.map(head, t -> matcher.match(t) ?
                tail.dropWhile(matcher) : this));
  }

  @Override
  public Stream<F, T> filter(Matcher1<T> matcher) {
    return suspendF(() ->
            monad.map(head, t -> matcher.match(t) ?
                cons(head, tail.filter(matcher)) : tail.filter(matcher)));
  }

  @Override
  public <R> Stream<F, R> collect(PartialFunction1<T, R> partial) {
    return suspendF(() ->
            monad.map(head, t -> partial.isDefinedAt(t) ?
                cons(monad.map(head, partial::apply), tail.collect(partial)) : tail.collect(partial)));
  }

  @Override
  public <R> Higher1<F, R> foldLeft(R begin, Function2<R, T, R> combinator) {
    return monad.flatMap(head, h -> tail.foldLeft(combinator.apply(begin, h), combinator));
  }

  @Override
  public <R> Higher1<F, R> foldRight(Higher1<F, R> begin, Function2<T, Higher1<F, R>, Higher1<F, R>> combinator) {
    return monad.flatMap(head, h -> tail.foldRight(combinator.apply(h, begin), combinator));
  }

  @Override
  public Higher1<F, Boolean> exists(Matcher1<T> matcher) {
    return foldRight(monad.pure(false), (t, acc) -> matcher.match(t) ? monad.pure(true) : acc);
  }

  @Override
  public Higher1<F, Boolean> forall(Matcher1<T> matcher) {
    return foldRight(monad.pure(true), (t, acc) -> matcher.match(t) ? acc : monad.pure(false));
  }

  @Override
  public <R> Stream<F, R> map(Function1<T, R> map) {
    return suspend(() -> cons(monad.map(head, map), suspend(() -> tail.map(map))));
  }

  @Override
  public <R> Stream<F, R> mapEval(Function1<T, Higher1<F, R>> mapper) {
    return suspend(() -> cons(monad.flatMap(head, mapper), suspend(() -> tail.mapEval(mapper))));
  }

  @Override
  public <R> Stream<F, R> flatMap(Function1<T, ? extends Higher2<Stream.µ, F, R>> map) {
    return suspendF(() ->
        monad.map(
            monad.map(head, map.andThen(Stream::narrowK)::apply),
            s -> s.concat(tail.flatMap(map))));
  }

  @Override
  public Stream<F, T> repeat() {
    return concat(suspend(this::repeat));
  }

  @Override
  public Stream<F, T> intersperse(Higher1<F, T> value) {
    return suspend(() -> cons(head, suspend(() -> cons(value, tail.intersperse(value)))));
  }

  @Override
  public Higher1<F, Option<Tuple2<T, Stream<F, T>>>> split() {
    return monad.map(head, t -> Option.some(Tuple2.of(t, tail)));
  }

  private <R> Stream<F, R> cons(Higher1<F, R> head, Stream<F, R> tail) {
    return new Cons<>(monad, defer, head, tail);
  }

  private <R> Stream<F, R> suspend(Producer<Stream<F, R>> stream) {
    return suspendF(stream.andThen(monad::pure));
  }

  private <R> Stream<F, R> suspendF(Producer<Higher1<F, Stream<F, R>>> stream) {
    return new Suspend<>(monad, defer, defer.defer(stream));
  }

  private Stream<F, T> empty() {
    return new Nil<>(monad, defer);
  }
}

final class Suspend<F extends Kind, T> implements Stream<F, T> {

  private final Monad<F> monad;
  private final Defer<F> defer;
  private final Higher1<F, Stream<F, T>> evalStream;

  Suspend(Monad<F> monad, Defer<F> defer, Higher1<F, Stream<F, T>> stream) {
    this.monad = requireNonNull(monad);
    this.defer = requireNonNull(defer);
    this.evalStream = requireNonNull(stream);
  }

  @Override
  public Stream<F, T> head() {
    return lazyMap(Stream::head);
  }

  @Override
  public Stream<F, T> tail() {
    return lazyMap(Stream::tail);
  }

  @Override
  public Higher1<F, Option<Cons<F, T>>> extract() {
    return monad.flatMap(evalStream, Stream::extract);
  }

  @Override
  public Stream<F, T> concat(Stream<F, T> other) {
    return lazyMap(s -> s.concat(other));
  }

  @Override
  public Stream<F, T> append(Higher1<F, T> other) {
    return lazyMap(s -> s.append(other));
  }

  @Override
  public Stream<F, T> prepend(Higher1<F, T> other) {
    return lazyMap(s -> s.prepend(other));
  }

  @Override
  public Stream<F, T> take(int n) {
    return lazyMap(s -> s.take(n));
  }

  @Override
  public Stream<F, T> drop(int n) {
    return lazyMap(s -> s.drop(n));
  }

  @Override
  public Stream<F, T> takeWhile(Matcher1<T> matcher) {
    return lazyMap(s -> s.takeWhile(matcher));
  }

  @Override
  public Stream<F, T> dropWhile(Matcher1<T> matcher) {
    return lazyMap(s -> s.dropWhile(matcher));
  }

  @Override
  public Stream<F, T> filter(Matcher1<T> matcher) {
    return lazyMap(s -> s.filter(matcher));
  }

  @Override
  public <R> Stream<F, R> collect(PartialFunction1<T, R> partial) {
    return lazyMap(s -> s.collect(partial));
  }

  @Override
  public <R> Higher1<F, R> foldLeft(R begin, Function2<R, T, R> combinator) {
    return monad.flatMap(evalStream, s -> s.foldLeft(begin, combinator));
  }

  @Override
  public <R> Higher1<F, R> foldRight(Higher1<F, R> begin, Function2<T, Higher1<F, R>, Higher1<F, R>> combinator) {
    return monad.flatMap(evalStream, s -> s.foldRight(begin, combinator));
  }

  @Override
  public Higher1<F, Boolean> exists(Matcher1<T> matcher) {
    return monad.flatMap(evalStream, s -> s.exists(matcher));
  }

  @Override
  public Higher1<F, Boolean> forall(Matcher1<T> matcher) {
    return monad.flatMap(evalStream, s -> s.forall(matcher));
  }

  @Override
  public <R> Stream<F, R> map(Function1<T, R> mapper) {
    return lazyMap(s -> s.map(mapper));
  }

  @Override
  public <R> Stream<F, R> mapEval(Function1<T, Higher1<F, R>> mapper) {
    return suspend(() -> monad.map(evalStream, s -> s.mapEval(mapper)));
  }

  @Override
  public <R> Stream<F, R> flatMap(Function1<T, ? extends Higher2<Stream.µ, F, R>> map) {
    return lazyMap(s -> s.flatMap(map));
  }

  @Override
  public Stream<F, T> repeat() {
    return lazyMap(s -> s.repeat());
  }

  @Override
  public Stream<F, T> intersperse(Higher1<F, T> value) {
    return lazyMap(s -> s.intersperse(value));
  }

  @Override
  public Higher1<F, Option<Tuple2<T, Stream<F, T>>>> split() {
    return monad.flatMap(evalStream, s -> s.split());
  }

  private <R> Stream<F, R> lazyMap(Function1<Stream<F, T>, Stream<F, R>> mapper) {
    return suspend(() -> monad.map(evalStream, mapper));
  }

  private <R> Stream<F, R> suspend(Producer<Higher1<F, Stream<F, R>>> stream) {
    return new Suspend<>(monad, defer, defer.defer(stream));
  }
}

final class Nil<F extends Kind, T> implements Stream<F, T> {

  private final Monad<F> monad;
  private final Defer<F> defer;

  Nil(Monad<F> monad, Defer<F> defer) {
    this.monad = requireNonNull(monad);
    this.defer = requireNonNull(defer);
  }

  @Override
  public Stream<F, T> head() {
    return this;
  }

  @Override
  public Stream<F, T> tail() {
    return this;
  }

  @Override
  public Higher1<F, Option<Cons<F, T>>> extract() {
    return monad.pure(Option.none());
  }

  @Override
  public Stream<F, T> take(int n) {
    return this;
  }

  @Override
  public Stream<F, T> drop(int n) {
    return this;
  }

  @Override
  public Stream<F, T> filter(Matcher1<T> matcher) {
    return this;
  }

  @Override
  public Stream<F, T> takeWhile(Matcher1<T> matcher) {
    return this;
  }

  @Override
  public Stream<F, T> dropWhile(Matcher1<T> matcher) {
    return this;
  }

  @Override
  public Stream<F, T> concat(Stream<F, T> other) {
    return other;
  }

  @Override
  public Stream<F, T> append(Higher1<F, T> other) {
    return new Cons<>(monad, defer, other, this);
  }

  @Override
  public Stream<F, T> prepend(Higher1<F, T> other) {
    return append(other);
  }

  @Override
  public <R> Stream<F, R> collect(PartialFunction1<T, R> partial) {
    return new Nil<>(monad, defer);
  }

  @Override
  public <R> Higher1<F, R> foldLeft(R begin, Function2<R, T, R> combinator) {
    return monad.pure(begin);
  }

  @Override
  public <R> Higher1<F, R> foldRight(Higher1<F, R> begin, Function2<T, Higher1<F, R>, Higher1<F, R>> combinator) {
    return begin;
  }

  @Override
  public Higher1<F, Boolean> exists(Matcher1<T> matcher) {
    return monad.pure(false);
  }

  @Override
  public Higher1<F, Boolean> forall(Matcher1<T> matcher) {
    return monad.pure(true);
  }

  @Override
  public <R> Stream<F, R> map(Function1<T, R> map) {
    return new Nil<>(monad, defer);
  }

  @Override
  public <R> Stream<F, R> mapEval(Function1<T, Higher1<F, R>> mapper) {
    return new Nil<>(monad, defer);
  }

  @Override
  public <R> Stream<F, R> flatMap(Function1<T, ? extends Higher2<Stream.µ, F, R>> map) {
    return new Nil<>(monad, defer);
  }

  @Override
  public Stream<F, T> repeat() {
    return this;
  }

  @Override
  public Stream<F, T> intersperse(Higher1<F, T> value) {
    return this;
  }

  @Override
  public Higher1<F, Option<Tuple2<T, Stream<F, T>>>> split() {
    return monad.pure(Option.none());
  }
}