/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.concurrent;

import com.github.tonivade.purefun.CheckedRunnable;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Matcher1;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Unit;

import java.util.concurrent.Executor;

import static com.github.tonivade.purefun.Function1.identity;

@HigherKind
@FunctionalInterface
public interface Par<T> {

  Future<T> apply(Executor executor);

  default Promise<T> run(Executor executor) {
    return apply(executor).toPromise();
  }

  default <R> Par<R> map(Function1<T, R> mapper) {
    return executor -> apply(executor).map(mapper);
  }

  default <R> Par<R> flatMap(Function1<T, Par<R>> mapper) {
    return executor -> apply(executor).flatMap(value -> mapper.apply(value).apply(executor));
  }

  default Par<T> filter(Matcher1<T> matcher) {
    return executor -> apply(executor).filter(matcher);
  }

  default Par<T> filterNot(Matcher1<T> matcher) {
    return executor -> apply(executor).filterNot(matcher);
  }

  default Par<T> recover(Function1<Throwable, T> recover) {
    return fold(recover, identity());
  }

  default <X extends Throwable> Par<T> recoverWith(Class<X> type, Function1<X, T> mapper) {
    return executor -> apply(executor).recoverWith(type, mapper);
  }

  default <R> Par<R> fold(Function1<Throwable, R> failureMapper, Function1<T, R> successmapper) {
    return executor -> apply(executor).fold(failureMapper, successmapper);
  }

  static <T> Par<T> success(T value) {
    return executor -> Future.success(executor, value);
  }

  static <T> Par<T> failure(Throwable error) {
    return executor -> Future.failure(executor, error);
  }

  static <T> Par<T> task(Producer<T> producer) {
    return executor -> Future.async(executor, producer);
  }

  static Par<Unit> run(CheckedRunnable runnable) {
    return executor -> Future.exec(executor, runnable);
  }
}

