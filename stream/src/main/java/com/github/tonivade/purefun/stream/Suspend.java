/*
 * Copyright (c) 2018-2022, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.stream;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Function2;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Witness;
import com.github.tonivade.purefun.Matcher1;
import com.github.tonivade.purefun.PartialFunction1;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.typeclasses.MonadDefer;

import static com.github.tonivade.purefun.Precondition.checkNonNull;

public final class Suspend<F extends Witness, T> implements PureStream<F, T> {

  private final MonadDefer<F> monad;
  private final Kind<F, PureStream<F, T>> evalStream;

  Suspend(MonadDefer<F> monad, Kind<F, PureStream<F, T>> stream) {
    this.monad = checkNonNull(monad);
    this.evalStream = checkNonNull(stream);
  }

  @Override
  public Kind<F, Option<T>> headOption() {
     return monad.flatMap(evalStream, PureStream::headOption);
  }

  @Override
  public Kind<F, Option<Tuple2<Kind<F, T>, PureStream<F, T>>>> split() {
    return monad.flatMap(evalStream, PureStream::split);
  }

  @Override
  public PureStream<F, T> concat(PureStream<F, ? extends T> other) {
    return lazyMap(s -> s.concat(other));
  }

  @Override
  public PureStream<F, T> append(Kind<F, ? extends T> other) {
    return lazyMap(s -> s.append(other));
  }

  @Override
  public PureStream<F, T> prepend(Kind<F, ? extends T> other) {
    return lazyMap(s -> s.prepend(other));
  }

  @Override
  public PureStream<F, T> take(int n) {
    return lazyMap(s -> s.take(n));
  }

  @Override
  public PureStream<F, T> drop(int n) {
    return lazyMap(s -> s.drop(n));
  }

  @Override
  public PureStream<F, T> takeWhile(Matcher1<? super T> matcher) {
    return lazyMap(s -> s.takeWhile(matcher));
  }

  @Override
  public PureStream<F, T> dropWhile(Matcher1<? super T> matcher) {
    return lazyMap(s -> s.dropWhile(matcher));
  }

  @Override
  public PureStream<F, T> filter(Matcher1<? super T> matcher) {
    return lazyMap(s -> s.filter(matcher));
  }

  @Override
  public <R> PureStream<F, R> collect(PartialFunction1<? super T, ? extends R> partial) {
    return lazyMap(s -> s.collect(partial));
  }

  @Override
  public <R> Kind<F, R> foldLeft(R begin, Function2<? super R, ? super T, ? extends R> combinator) {
    return monad.flatMap(evalStream, s -> s.foldLeft(begin, combinator));
  }

  @Override
  public <R> Kind<F, R> foldRight(Kind<F, ? extends R> begin, 
      Function2<? super T, ? super Kind<F, ? extends R>, ? extends Kind<F, ? extends R>> combinator) {
    return monad.flatMap(evalStream, s -> s.foldRight(begin, combinator));
  }

  @Override
  public Kind<F, Boolean> exists(Matcher1<? super T> matcher) {
    return monad.flatMap(evalStream, s -> s.exists(matcher));
  }

  @Override
  public Kind<F, Boolean> forall(Matcher1<? super T> matcher) {
    return monad.flatMap(evalStream, s -> s.forall(matcher));
  }

  @Override
  public <R> PureStream<F, R> map(Function1<? super T, ? extends R> mapper) {
    return lazyMap(s -> s.map(mapper));
  }

  @Override
  public <R> PureStream<F, R> mapEval(Function1<? super T, ? extends Kind<F, ? extends R>> mapper) {
    return suspend(() -> monad.map(evalStream, s -> s.mapEval(mapper)));
  }

  @Override
  public <R> PureStream<F, R> flatMap(Function1<? super T, ? extends Kind<Kind<PureStream_, F>, ? extends R>> map) {
    return lazyMap(s -> s.flatMap(map));
  }

  @Override
  public PureStream<F, T> repeat() {
    return lazyMap(PureStream::repeat);
  }

  @Override
  public PureStream<F, T> intersperse(Kind<F, ? extends T> value) {
    return lazyMap(s -> s.intersperse(value));
  }

  private <R> PureStream<F, R> lazyMap(Function1<PureStream<F, T>, PureStream<F, R>> mapper) {
    return suspend(() -> monad.map(evalStream, mapper));
  }

  private <R> PureStream<F, R> suspend(Producer<Kind<F, PureStream<F, R>>> stream) {
    return new Suspend<>(monad, monad.defer(stream));
  }
}
