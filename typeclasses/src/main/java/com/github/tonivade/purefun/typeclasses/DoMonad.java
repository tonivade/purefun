/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.Unit.unit;
import static java.util.Objects.requireNonNull;

import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Function2;
import com.github.tonivade.purefun.Function3;
import com.github.tonivade.purefun.Function4;
import com.github.tonivade.purefun.Function5;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Unit;

public final class DoMonad<F extends Kind, A> {

  private final Monad<F> monad;
  private final Higher1<F, A> value;

  private DoMonad(Monad<F> monad, Higher1<F, A> value) {
    this.monad = requireNonNull(monad);
    this.value = requireNonNull(value);
  }

  public Higher1<F, A> get() {
    return value;
  }

  public <R> R fix(Function1<Higher1<F, A>, R> mapper) {
    return mapper.apply(value);
  }

  public void end(Consumer1<Higher1<F, A>> consumer) {
     consumer.accept(value);
  }

  public <R> Higher1<F, R> returns(R value) {
    return monad.pure(value);
  }

  public <R> DoMonad<F, R> map(Function1<A, R> mapper) {
    return with(monad, monad.map(value, mapper));
  }

  public <R> DoMonad<F, R> andThen(Producer<Higher1<F, R>> producer) {
    return with(monad, monad.flatMap(value, i -> producer.get()));
  }

  public <B, R> DoMonad<F, R> map2(Higher1<F, B> value2, Function2<A, B, R> mapper) {
    return with(monad, monad.map2(value, value2, mapper));
  }

  public <B, C, R> DoMonad<F, R> map3(Higher1<F, B> value2, Higher1<F, C> value3,
      Function3<A, B, C, R> mapper) {
    return with(monad, monad.map3(value, value2, value3, mapper));
  }

  public <B, C, D, R> DoMonad<F, R> map4(Higher1<F, B> value2, Higher1<F, C> value3,
      Higher1<F, D> value4, Function4<A, B, C, D, R> mapper) {
    return with(monad, monad.map4(value, value2, value3, value4, mapper));
  }

  public <B, C, D, E, R> DoMonad<F, R> map5(Higher1<F, B> value2, Higher1<F, C> value3,
      Higher1<F, D> value4, Higher1<F, E> value5, Function5<A, B, C, D, E, R> mapper) {
    return with(monad, monad.map5(value, value2, value3, value4, value5, mapper));
  }

  public <R> DoMonad<F, R> ap(Higher1<F, Function1<A, R>> apply) {
    return with(monad, monad.ap(value, apply));
  }

  public <R> DoMonad<F, R> flatMap(Function1<A, ? extends Higher1<F, R>> mapper) {
    return with(monad, monad.flatMap(value, mapper));
  }

  public static <F extends Kind> DoMonad<F, Unit> with(Monad<F> monad) {
    return new DoMonad<>(monad, monad.pure(unit()));
  }

  public static <F extends Kind, T> DoMonad<F, T> with(Monad<F> monad, Higher1<F, T> value) {
    return new DoMonad<>(monad, value);
  }
}
