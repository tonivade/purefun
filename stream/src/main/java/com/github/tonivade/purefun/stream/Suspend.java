/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.stream;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Function2;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Matcher1;
import com.github.tonivade.purefun.PartialFunction1;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.typeclasses.MonadDefer;

import static com.github.tonivade.purefun.Precondition.checkNonNull;

final class Suspend<F extends Kind, T> implements Stream<F, T> {

  private final MonadDefer<F> monad;
  private final Higher1<F, Stream<F, T>> evalStream;

  Suspend(MonadDefer<F> monad, Higher1<F, Stream<F, T>> stream) {
    this.monad = checkNonNull(monad);
    this.evalStream = checkNonNull(stream);
  }

  @Override
  public Higher1<F, Option<T>> headOption() {
     return monad.flatMap(evalStream, Stream::headOption);
  }

  @Override
  public Higher1<F, Option<Tuple2<Higher1<F, T>, Stream<F, T>>>> split() {
    return monad.flatMap(evalStream, Stream::split);
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
  public <R> Stream<F, R> flatMap(Function1<T, Stream<F, R>> map) {
    return lazyMap(s -> s.flatMap(map));
  }

  @Override
  public Stream<F, T> repeat() {
    return lazyMap(Stream::repeat);
  }

  @Override
  public Stream<F, T> intersperse(Higher1<F, T> value) {
    return lazyMap(s -> s.intersperse(value));
  }

  @Override
  public StreamModule getModule() { throw new UnsupportedOperationException(); }

  private <R> Stream<F, R> lazyMap(Function1<Stream<F, T>, Stream<F, R>> mapper) {
    return suspend(() -> monad.map(evalStream, mapper));
  }

  private <R> Stream<F, R> suspend(Producer<Higher1<F, Stream<F, R>>> stream) {
    return new Suspend<>(monad, monad.defer(stream));
  }
}
