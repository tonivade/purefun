/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.stream;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Function2;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Witness;
import com.github.tonivade.purefun.Matcher1;
import com.github.tonivade.purefun.PartialFunction1;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.typeclasses.MonadDefer;

import static com.github.tonivade.purefun.Precondition.checkNonNull;

final class Nil<F extends Witness, T> implements SealedStream<F, T> {

  private final MonadDefer<F> monad;

  Nil(MonadDefer<F> monad) {
    this.monad = checkNonNull(monad);
  }

  @Override
  public Kind<F, Option<T>> headOption() {
    return monad.pure(Option.none());
  }

  @Override
  public Kind<F, Option<Tuple2<Kind<F, T>, Stream<F, T>>>> split() {
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
  public Stream<F, T> append(Kind<F, T> other) {
    return new Cons<>(monad, other, this);
  }

  @Override
  public Stream<F, T> prepend(Kind<F, T> other) {
    return append(other);
  }

  @Override
  public <R> Stream<F, R> collect(PartialFunction1<T, R> partial) {
    return new Nil<>(monad);
  }

  @Override
  public <R> Kind<F, R> foldLeft(R begin, Function2<R, T, R> combinator) {
    return monad.pure(begin);
  }

  @Override
  public <R> Kind<F, R> foldRight(Kind<F, R> begin, Function2<T, Kind<F, R>, Kind<F, R>> combinator) {
    return begin;
  }

  @Override
  public Kind<F, Boolean> exists(Matcher1<T> matcher) {
    return monad.pure(false);
  }

  @Override
  public Kind<F, Boolean> forall(Matcher1<T> matcher) {
    return monad.pure(true);
  }

  @Override
  public <R> Stream<F, R> map(Function1<? super T, ? extends R> map) {
    return new Nil<>(monad);
  }

  @Override
  public <R> Stream<F, R> mapEval(Function1<T, Kind<F, R>> mapper) {
    return new Nil<>(monad);
  }

  @Override
  public <R> Stream<F, R> flatMap(Function1<T, Stream<F, R>> map) {
    return new Nil<>(monad);
  }

  @Override
  public Stream<F, T> repeat() {
    return this;
  }

  @Override
  public Stream<F, T> intersperse(Kind<F, T> value) {
    return this;
  }
}
