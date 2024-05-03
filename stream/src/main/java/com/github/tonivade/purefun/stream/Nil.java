/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.stream;

import static com.github.tonivade.purefun.core.Precondition.checkNonNull;

import com.github.tonivade.purefun.Kind;

import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Function2;
import com.github.tonivade.purefun.core.Matcher1;
import com.github.tonivade.purefun.core.PartialFunction1;
import com.github.tonivade.purefun.core.Tuple2;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.typeclasses.MonadDefer;

public final class Nil<F, T> implements PureStream<F, T> {

  private final MonadDefer<F> monad;

  Nil(MonadDefer<F> monad) {
    this.monad = checkNonNull(monad);
  }

  @Override
  public Kind<F, Option<T>> headOption() {
    return monad.pure(Option.none());
  }

  @Override
  public Kind<F, Option<Tuple2<Kind<F, T>, PureStream<F, T>>>> split() {
    return monad.pure(Option.none());
  }

  @Override
  public PureStream<F, T> take(int n) {
    return this;
  }

  @Override
  public PureStream<F, T> drop(int n) {
    return this;
  }

  @Override
  public PureStream<F, T> filter(Matcher1<? super T> matcher) {
    return this;
  }

  @Override
  public PureStream<F, T> takeWhile(Matcher1<? super T> matcher) {
    return this;
  }

  @Override
  public PureStream<F, T> dropWhile(Matcher1<? super T> matcher) {
    return this;
  }

  @SuppressWarnings("unchecked")
  @Override
  public PureStream<F, T> concat(PureStream<F, ? extends T> other) {
    return (PureStream<F, T>) other;
  }

  @Override
  public PureStream<F, T> append(Kind<F, ? extends T> other) {
    return new Cons<>(monad, Kind.narrowK(other), this);
  }

  @Override
  public PureStream<F, T> prepend(Kind<F, ? extends T> other) {
    return append(other);
  }

  @Override
  public <R> PureStream<F, R> collect(PartialFunction1<? super T, ? extends R> partial) {
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
  public <R> PureStream<F, R> map(Function1<? super T, ? extends R> map) {
    return new Nil<>(monad);
  }

  @Override
  public <R> PureStream<F, R> mapEval(Function1<? super T, ? extends Kind<F, ? extends R>> mapper) {
    return new Nil<>(monad);
  }

  @Override
  public <R> PureStream<F, R> flatMap(Function1<? super T, ? extends Kind<Kind<PureStream<?, ?>, F>, ? extends R>> map) {
    return new Nil<>(monad);
  }

  @Override
  public PureStream<F, T> repeat() {
    return this;
  }

  @Override
  public PureStream<F, T> intersperse(Kind<F, ? extends T> value) {
    return this;
  }
}
