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
  public Stream<F, T> filter(Matcher1<? super T> matcher) {
    return this;
  }

  @Override
  public Stream<F, T> takeWhile(Matcher1<? super T> matcher) {
    return this;
  }

  @Override
  public Stream<F, T> dropWhile(Matcher1<? super T> matcher) {
    return this;
  }

  @SuppressWarnings("unchecked")
  @Override
  public Stream<F, T> concat(Stream<F, ? extends T> other) {
    return (Stream<F, T>) other;
  }

  @Override
  public Stream<F, T> append(Kind<F, ? extends T> other) {
    return new Cons<>(monad, Kind.<F, T>narrowK(other), this);
  }

  @Override
  public Stream<F, T> prepend(Kind<F, ? extends T> other) {
    return append(other);
  }

  @Override
  public <R> Stream<F, R> collect(PartialFunction1<? super T, ? extends R> partial) {
    return new Nil<>(monad);
  }

  @Override
  public <R> Kind<F, R> foldLeft(R begin, Function2<? super R, ? super T, ? extends R> combinator) {
    return monad.pure(begin);
  }

  @Override
  public <R> Kind<F, R> foldRight(Kind<F, ? extends R> begin, 
      Function2<? super T, ? super Kind<F, ? extends R>, ? extends Kind<F, ? extends R>> combinator) {
    return Kind.narrowK(begin);
  }

  @Override
  public Kind<F, Boolean> exists(Matcher1<? super T> matcher) {
    return monad.pure(false);
  }

  @Override
  public Kind<F, Boolean> forall(Matcher1<? super T> matcher) {
    return monad.pure(true);
  }

  @Override
  public <R> Stream<F, R> map(Function1<? super T, ? extends R> map) {
    return new Nil<>(monad);
  }

  @Override
  public <R> Stream<F, R> mapEval(Function1<? super T, ? extends Kind<F, ? extends R>> mapper) {
    return new Nil<>(monad);
  }

  @Override
  public <R> Stream<F, R> flatMap(Function1<? super T, ? extends Stream<F, ? extends R>> map) {
    return new Nil<>(monad);
  }

  @Override
  public Stream<F, T> repeat() {
    return this;
  }

  @Override
  public Stream<F, T> intersperse(Kind<F, ? extends T> value) {
    return this;
  }
}
