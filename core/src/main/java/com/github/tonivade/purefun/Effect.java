/*
 * Copyright (c) 2018-2021, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import java.time.Duration;

public interface Effect<F extends Witness, A> extends Bindable<F, A>, Applicable<F, A> {
  
  @Override
  <R> Effect<F, R> map(Function1<? super A, ? extends R> mapper);

  @Override
  <R> Effect<F, R> ap(Kind<F, Function1<? super A, ? extends R>> apply);

  @Override
  <R> Effect<F, R> flatMap(Function1<? super A, ? extends Kind<F, ? extends R>> mapper);

  default <R> Effect<F, R> andThen(Kind<F, ? extends R> next) {
    return flatMap(ignore -> next);
  }

  Effect<F, Tuple2<Duration, A>> timed();

  default Effect<F, A> repeat() {
    return repeat(1);
  }

  Effect<F, A> repeat(int times);

  default Effect<F, A> repeat(Duration delay) {
    return repeat(delay, 1);
  }

  Effect<F, A> repeat(Duration delay, int times);

  default Effect<F, A> retry() {
    return retry(1);
  }

  Effect<F, A> retry(int maxRetries);

  default Effect<F, A> retry(Duration delay) {
    return retry(delay, 1);
  }

  Effect<F, A> retry(Duration delay, int maxRetries);
  
  @SuppressWarnings("unchecked")
  static <F extends Witness, A> Effect<F, A> narrowK(Kind<F, ? extends A> kind) {
    return (Effect<F, A>) kind;
  }
}
