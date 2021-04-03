/*
 * Copyright (c) 2018-2021, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import java.time.Duration;

public interface Effect<F extends Witness, A> extends Kind<F, A> {
  
  <R> Effect<F, R> map(Function1<? super A, ? extends R> mapper);

  <R> Effect<F, R> ap(Kind<F, Function1<? super A, ? extends R>> apply);

  <R> Effect<F, R> flatMap(Function1<? super A, ? extends Kind<F, ? extends R>> mapper);

  <R> Effect<F, R> andThen(Kind<F, ? extends R> next);

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

  default <B> Effect<F, Tuple2<A, B>> zip(Kind<F, ? extends B> other) {
    return zipWith(other, Tuple::of);
  }

  default <B> Effect<F, A> zipLeft(Kind<F, ? extends B> other) {
    return zipWith(other, Function2.first());
  }

  default <B> Effect<F, B> zipRight(Kind<F, ? extends B> other) {
    return zipWith(other, Function2.second());
  }

  default <B, C> Effect<F, C> zipWith(Kind<F, ? extends B> other, Function2<? super A, ? super B, ? extends C> mapper) {
    return map2(this, narrowK(other), mapper);
  }

  static <F extends Witness, A, B, C> Effect<F, C> map2(Effect<F, ? extends A> fa, Effect<F, ? extends B> fb, 
      Function2<? super A, ? super B, ? extends C> mapper) {
    return fb.ap(fa.map(mapper.curried()));
  }
  
  @SuppressWarnings("unchecked")
  static <F extends Witness, A> Effect<F, A> narrowK(Kind<F, ? extends A> kind) {
    return (Effect<F, A>) kind;
  }
}
