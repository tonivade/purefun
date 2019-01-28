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
import com.github.tonivade.purefun.data.ImmutableList;
import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.type.Eval;
import com.github.tonivade.purefun.typeclasses.Comonad;
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

  default Higher1<F, Sequence<T>> asSequence() {
    return foldLeft(ImmutableList.empty(), (acc, a) -> acc.append(a));
  }

  default Higher1<F, String> asString() {
    return foldLeft("", (acc, a) -> acc + a);
  }

  default Higher1<F, Nothing> drain() {
    return foldLeft(nothing(), (acc, a) -> acc);
  }

  static <F extends Kind, T> Stream<F, T> empty(Monad<F> monad, Comonad<F> comonad) {
    return new Nil<>(monad, comonad);
  }

  @SafeVarargs
  static <F extends Kind, T> Stream<F, T> of(Monad<F> monad, Comonad<F> comonad, T... values) {
    return from(monad, comonad, Arrays.stream(values));
  }

  static <F extends Kind, T> Stream<F, T> pure(Monad<F> monad, Comonad<F> comonad, T value) {
    return eval(monad, comonad, monad.pure(value));
  }

  static <F extends Kind, T> Stream<F, T> eval(Monad<F> monad, Comonad<F> comonad, Higher1<F, T> value) {
    return new Cons<>(monad, comonad, value, empty(monad, comonad));
  }

  static <F extends Kind, T> Stream<F, T> from(Monad<F> monad, Comonad<F> comonad, Iterable<T> iterable) {
    return from(monad, comonad, asStream(iterable.iterator()));
  }

  static <F extends Kind, T> Stream<F, T> from(Monad<F> monad, Comonad<F> comonad, Sequence<T> sequence) {
    return sequence.foldLeft(Stream.<F, T>empty(monad, comonad),
        (acc, a) -> acc.append(monad.pure(a)));
  }

  static <F extends Kind, T> Stream<F, T> from(Monad<F> monad, Comonad<F> comonad, java.util.stream.Stream<T> stream) {
    return from(monad, comonad, ImmutableList.from(stream));
  }

  static <F extends Kind, T> Stream<F, T> iterate(Monad<F> monad, Comonad<F> comonad, T seed, Operator1<T> generator) {
    return new Cons<>(monad, comonad, monad.pure(seed),
        new Defer<>(monad, () -> iterate(monad, comonad, generator.apply(seed), generator)));
  }

  static <F extends Kind, T> Stream<F, T> iterate(Monad<F> monad, Comonad<F> comonad, Producer<T> generator) {
    return new Cons<>(monad, comonad, monad.pure(generator.get()), 
        new Defer<>(monad, () -> iterate(monad, comonad, generator)));
  }

  static <F extends Kind, T> Stream<F, T> narrowK(Higher1<Higher1<Stream.µ, F>, T> hkt) {
    return (Stream<F, T>) hkt;
  }

  static <F extends Kind, T> Stream<F, T> narrowK(Higher2<Stream.µ, F, T> hkt) {
    return (Stream<F, T>) hkt;
  }
}

final class Cons<F extends Kind, T> implements Stream<F, T> {

  private final Monad<F> monad;
  private final Comonad<F> comonad;
  private final Higher1<F, T> head;
  private final Stream<F, T> tail;

  Cons(Monad<F> monad, Comonad<F> comonad, Higher1<F, T> head, Stream<F, T> tail) {
    this.monad = requireNonNull(monad);
    this.comonad = requireNonNull(comonad);
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
  public Stream<F, T> concat(Stream<F, T> other) {
    return defer(() -> cons(head, tail.concat(other)));
  }

  @Override
  public Stream<F, T> append(Higher1<F, T> other) {
    return defer(() -> cons(head, tail.append(other)));
  }

  @Override
  public Stream<F, T> prepend(Higher1<F, T> other) {
    return defer(() -> cons(other, tail.prepend(head)));
  }

  @Override
  public Stream<F, T> take(int n) {
    return n > 0 ? defer(() -> cons(head, tail.take(n - 1))) : empty();
  }

  @Override
  public Stream<F, T> drop(int n) {
    return n > 0 ? defer(() -> tail.drop(n - 1)) : this;
  }

  @Override
  public Stream<F, T> takeWhile(Matcher1<T> matcher) {
    return defer(() -> comonad.extract(
        monad.map(head, t -> matcher.match(t) ?
            cons(head, tail.takeWhile(matcher)) : empty())));
  }

  @Override
  public Stream<F, T> dropWhile(Matcher1<T> matcher) {
    return defer(() -> comonad.extract(
            monad.map(head, t -> matcher.match(t) ?
                tail.dropWhile(matcher) : this)));
  }

  @Override
  public Stream<F, T> filter(Matcher1<T> matcher) {
    return defer(() -> comonad.extract(
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
    return defer(() -> cons(monad.map(head, map), defer(() -> tail.map(map))));
  }

  @Override
  public <R> Stream<F, R> mapEval(Function1<T, Higher1<F, R>> mapper) {
    return defer(() -> cons(monad.flatMap(head, mapper), defer(() -> tail.mapEval(mapper))));
  }

  @Override
  public <R> Stream<F, R> flatMap(Function1<T, ? extends Higher2<Stream.µ, F, R>> map) {
    return defer(() -> comonad.extract(
        monad.map(
            monad.map(head, map.andThen(Stream::narrowK)::apply),
            s -> s.concat(tail.flatMap(map)))));
  }

  @Override
  public Stream<F, T> repeat() {
    return concat(defer(this::repeat));
  }

  @Override
  public Stream<F, T> intersperse(Higher1<F, T> value) {
    return defer(() -> cons(head, defer(() -> cons(value, tail.intersperse(value)))));
  }

  private <R> Stream<F, R> cons(Higher1<F, R> head, Stream<F, R> tail) {
    return new Cons<>(monad, comonad, head, tail);
  }

  private <R> Stream<F, R> defer(Producer<Stream<F, R>> stream) {
    return new Defer<>(monad, later(stream));
  }

  private Stream<F, T> empty() {
    return Stream.empty(monad, comonad);
  }
}

final class Defer<F extends Kind, T> implements Stream<F, T> {

  private final Monad<F> monad;
  private final Eval<Stream<F, T>> evalStream;

  Defer(Monad<F> monad, Eval<Stream<F, T>> stream) {
    this.monad = requireNonNull(monad);
    this.evalStream = requireNonNull(stream);
  }

  @Override
  public Stream<F, T> head() {
    return defer(evalStream.map(Stream::head));
  }

  @Override
  public Stream<F, T> tail() {
    return defer(evalStream.map(Stream::tail));
  }

  @Override
  public Stream<F, T> concat(Stream<F, T> other) {
    return defer(evalStream.map(s -> s.concat(other)));
  }

  @Override
  public Stream<F, T> append(Higher1<F, T> other) {
    return defer(evalStream.map(s -> s.append(other)));
  }

  @Override
  public Stream<F, T> prepend(Higher1<F, T> other) {
    return defer(evalStream.map(s -> s.prepend(other)));
  }

  @Override
  public Stream<F, T> take(int n) {
    return defer(evalStream.map(s -> s.take(n)));
  }

  @Override
  public Stream<F, T> drop(int n) {
    return defer(evalStream.map(s -> s.drop(n)));
  }

  @Override
  public Stream<F, T> takeWhile(Matcher1<T> matcher) {
    return defer(evalStream.map(s -> s.takeWhile(matcher)));
  }

  @Override
  public Stream<F, T> dropWhile(Matcher1<T> matcher) {
    return defer(evalStream.map(s -> s.dropWhile(matcher)));
  }

  @Override
  public Stream<F, T> filter(Matcher1<T> matcher) {
    return defer(evalStream.map(s -> s.filter(matcher)));
  }

  @Override
  public <R> Higher1<F, R> foldLeft(R begin, Function2<R, T, R> combinator) {
    /*
     * TODO: this method is not really lazy, it evaluates all the stream.
     * This is because monads don't compose
     */
    return evalStream.flatMap(s -> later(() -> s.foldLeft(begin, combinator))).value();
  }

  @Override
  public <R> Eval<Higher1<F, R>> foldRight(Eval<R> begin, Function2<T, Eval<R>, Eval<R>> combinator) {
    return evalStream.flatMap(s -> s.foldRight(begin, combinator));
  }

  @Override
  public <R> Stream<F, R> map(Function1<T, R> map) {
    return defer(evalStream.map(s -> s.map(map)));
  }

  @Override
  public <R> Stream<F, R> mapEval(Function1<T, Higher1<F, R>> mapper) {
    return defer(evalStream.map(s -> s.mapEval(mapper)));
  }

  @Override
  public <R> Stream<F, R> flatMap(Function1<T, ? extends Higher2<µ, F, R>> map) {
    return defer(evalStream.map(s -> s.flatMap(map)));
  }

  @Override
  public Stream<F, T> repeat() {
    return defer(evalStream.map(s -> s.repeat()));
  }

  @Override
  public Stream<F, T> intersperse(Higher1<F, T> value) {
    return defer(evalStream.map(s -> s.intersperse(value)));
  }

  private <R> Stream<F, R> defer(Eval<Stream<F, R>> stream) {
    return new Defer<>(monad, stream);
  }
}

final class Nil<F extends Kind, T> implements Stream<F, T> {

  private final Monad<F> monad;
  private final Comonad<F> comonad;

  Nil(Monad<F> monad, Comonad<F> comonad) {
    this.monad = requireNonNull(monad);
    this.comonad = requireNonNull(comonad);
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
    return new Cons<>(monad, comonad, other, this);
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
    return new Nil<>(monad, comonad);
  }

  @Override
  public <R> Stream<F, R> mapEval(Function1<T, Higher1<F, R>> mapper) {
    return new Nil<>(monad, comonad);
  }

  @Override
  public <R> Stream<F, R> flatMap(Function1<T, ? extends Higher2<Stream.µ, F, R>> map) {
    return new Nil<>(monad, comonad);
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