/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.stream;

import static com.github.tonivade.purefun.Nothing.nothing;
import static com.github.tonivade.purefun.data.Sequence.asStream;
import static com.github.tonivade.purefun.type.Eval.later;
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
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.data.ImmutableList;
import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.monad.IO;
import com.github.tonivade.purefun.type.Eval;
import com.github.tonivade.purefun.type.Id;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.typeclasses.Comonad;
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

  <R> Higher1<F, R> foldLeft(R begin, Function2<R, T, R> combinator);
  <R> Eval<Higher1<F, R>> foldRight(Eval<R> begin, Function2<T, Eval<R>, Eval<R>> combinator);

  @Override
  <R> Stream<F, R> map(Function1<T, R> map);
  @Override
  <R> Stream<F, R> flatMap(Function1<T, ? extends Higher2<Stream.µ, F, R>> map);
  <R> Stream<F, R> mapEval(Function1<T, Higher1<F, R>> mapper);

  Stream<F, T> repeat();
  Stream<F, T> intersperse(Higher1<F, T> value);

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

  static StreamOf<IO.µ> ofIO() {
    return of(IO.monad(), IO.comonad(), IO.defer());
  }

  static StreamOf<Id.µ> ofId() {
    return of(Id.monad(), Id.comonad(), Id.defer());
  }

  static <F extends Kind> StreamOf<F> of(Monad<F> monad, Comonad<F> comonad, Defer<F> defer) {
    return new StreamOf<F>() {

      @Override
      public Monad<F> monad() { return monad; }

      @Override
      public Comonad<F> comonad() { return comonad; }

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
    Comonad<F> comonad();
    Defer<F> defer();

    default <T> Stream<F, T> empty() {
      return new Nil<>(monad(), comonad(), defer());
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
      return new Cons<>(monad(), comonad(), defer(), value, empty());
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
      return new Cons<>(monad(), comonad(), defer(), monad().pure(seed),
          new Suspend<>(monad(), defer(), defer().defer(
              () -> monad().pure(iterate(generator.apply(seed), generator)))));
    }

    default <T> Stream<F, T> iterate(Producer<T> generator) {
      return new Cons<>(monad(), comonad(), defer(), monad().pure(generator.get()),
          new Suspend<>(monad(), defer(), defer().defer(
              () -> monad().pure(iterate(generator)))));
    }

    default <A, B, R> Stream<F, R> zipWith(Stream<F, A> s1, Stream<F, B> s2, Function2<A, B, R> combinator) {
      return new Suspend<>(monad(), defer(), defer().defer(
          () -> monad().map2(s1.extract(), s2.extract(),
              (op1, op2) -> Option.<Stream<F, R>>narrowK(Option.monad().map2(op1, op2,
                  (cons1, cons2) -> new Cons<>(monad(), comonad(), defer(),
                      monad().map2(cons1.head, cons2.head, combinator),
                      zipWith(cons1.tail, cons2.tail, combinator)))).getOrElse(empty()))));
    }

    default <A, B> Stream<F, Tuple2<A, B>> zip(Stream<F, A> s1, Stream<F, B> s2) {
      return zipWith(s1, s2, Tuple2::of);
    }

    default <A> Stream<F, Tuple2<A, Integer>> zipWithIndex(Stream<F, A> stream) {
      return zip(stream, iterate(0, x -> x + 1));
    }
  }
}

final class Cons<F extends Kind, T> implements Stream<F, T> {

  private final Monad<F> monad;
  private final Comonad<F> comonad;
  private final Defer<F> defer;
  final Higher1<F, T> head;
  final Stream<F, T> tail;

  Cons(Monad<F> monad, Comonad<F> comonad, Defer<F> defer, Higher1<F, T> head, Stream<F, T> tail) {
    this.monad = requireNonNull(monad);
    this.comonad = requireNonNull(comonad);
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
    return suspend(() -> comonad.extract(
        monad.map(head, t -> matcher.match(t) ?
            cons(head, tail.takeWhile(matcher)) : empty())));
  }

  @Override
  public Stream<F, T> dropWhile(Matcher1<T> matcher) {
    return suspend(() -> comonad.extract(
            monad.map(head, t -> matcher.match(t) ?
                tail.dropWhile(matcher) : this)));
  }

  @Override
  public Stream<F, T> filter(Matcher1<T> matcher) {
    return suspend(() -> comonad.extract(
            monad.map(head, t -> matcher.match(t) ?
                cons(head, tail.filter(matcher)) : tail.filter(matcher))));
  }

  @Override
  public <R> Higher1<F, R> foldLeft(R begin, Function2<R, T, R> combinator) {
    return monad.flatMap(head, h -> tail.foldLeft(combinator.apply(begin, h), combinator));
  }

  @Override
  public <R> Eval<Higher1<F, R>> foldRight(Eval<R> begin, Function2<T, Eval<R>, Eval<R>> combinator) {
    return later(() -> monad.flatMap(
        head, h -> tail.foldRight(combinator.apply(h, begin), combinator).value()));
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
    return suspend(() -> comonad.extract(
        monad.map(
            monad.map(head, map.andThen(Stream::narrowK)::apply),
            s -> s.concat(tail.flatMap(map)))));
  }

  @Override
  public Stream<F, T> repeat() {
    return concat(suspend(this::repeat));
  }

  @Override
  public Stream<F, T> intersperse(Higher1<F, T> value) {
    return suspend(() -> cons(head, suspend(() -> cons(value, tail.intersperse(value)))));
  }

  private <R> Stream<F, R> cons(Higher1<F, R> head, Stream<F, R> tail) {
    return new Cons<>(monad, comonad, defer, head, tail);
  }

  private <R> Stream<F, R> suspend(Producer<Stream<F, R>> stream) {
    return new Suspend<>(monad, defer, defer.defer(stream.andThen(monad::pure)));
  }

  private Stream<F, T> empty() {
    return new Nil<>(monad, comonad, defer);
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
  public <R> Higher1<F, R> foldLeft(R begin, Function2<R, T, R> combinator) {
    return monad.flatMap(evalStream, s -> s.foldLeft(begin, combinator));
  }

  @Override
  public <R> Eval<Higher1<F, R>> foldRight(Eval<R> begin, Function2<T, Eval<R>, Eval<R>> combinator) {
    return later(() -> monad.flatten(monad.map(evalStream, s -> s.foldRight(begin, combinator).value())));
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
  public <R> Stream<F, R> flatMap(Function1<T, ? extends Higher2<µ, F, R>> map) {
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

  private <R> Stream<F, R> lazyMap(Function1<Stream<F, T>, Stream<F, R>> mapper) {
    return suspend(() -> monad.map(evalStream, mapper));
  }

  private <R> Stream<F, R> suspend(Producer<Higher1<F, Stream<F, R>>> stream) {
    return new Suspend<>(monad, defer, defer.defer(stream));
  }
}

final class Nil<F extends Kind, T> implements Stream<F, T> {

  private final Monad<F> monad;
  private final Comonad<F> comonad;
  private final Defer<F> defer;

  Nil(Monad<F> monad, Comonad<F> comonad, Defer<F> defer) {
    this.monad = requireNonNull(monad);
    this.comonad = requireNonNull(comonad);
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
    return new Cons<>(monad, comonad, defer, other, this);
  }

  @Override
  public Stream<F, T> prepend(Higher1<F, T> other) {
    return append(other);
  }

  @Override
  public <R> Higher1<F, R> foldLeft(R begin, Function2<R, T, R> combinator) {
    return monad.pure(begin);
  }

  @Override
  public <R> Eval<Higher1<F, R>> foldRight(Eval<R> begin, Function2<T, Eval<R>, Eval<R>> combinator) {
    return begin.map(monad::pure);
  }

  @Override
  public <R> Stream<F, R> map(Function1<T, R> map) {
    return new Nil<>(monad, comonad, defer);
  }

  @Override
  public <R> Stream<F, R> mapEval(Function1<T, Higher1<F, R>> mapper) {
    return new Nil<>(monad, comonad, defer);
  }

  @Override
  public <R> Stream<F, R> flatMap(Function1<T, ? extends Higher2<Stream.µ, F, R>> map) {
    return new Nil<>(monad, comonad, defer);
  }

  @Override
  public Stream<F, T> repeat() {
    return this;
  }

  @Override
  public Stream<F, T> intersperse(Higher1<F, T> value) {
    return this;
  }
}