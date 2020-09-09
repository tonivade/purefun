/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.concurrent;

import static com.github.tonivade.purefun.Function1.identity;
import static com.github.tonivade.purefun.Function2.second;
import static com.github.tonivade.purefun.data.ImmutableList.empty;
import java.time.Duration;
import java.util.concurrent.Executor;
import com.github.tonivade.purefun.CheckedRunnable;
import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Function2;
import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Matcher1;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Tuple;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.type.Try;

@HigherKind
@FunctionalInterface
public interface Par<T> extends ParOf<T> {

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

  default <R> Par<R> andThen(Par<R> next) {
    return map2(this, next, second());
  }

  default <R> Par<R> ap(Par<Function1<T, R>> apply) {
    return executor -> apply(executor).ap(apply.apply(executor));
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
    return executor -> Future.task(executor, producer);
  }

  static <T> Par<T> defer(Producer<Par<T>> producer) {
    return executor -> Future.defer(executor, () -> producer.get().apply(executor));
  }

  static Par<Unit> run(CheckedRunnable runnable) {
    return executor -> Future.exec(executor, runnable);
  }

  static <T> Par<T> async(Consumer1<Consumer1<Try<T>>> consumer) {
    return executor -> Future.async(executor, consumer);
  }

  static Par<Unit> sleep(Duration delay) {
    return executor -> Future.sleep(executor, delay);
  }

  static <A, B> Par<B> bracket(Par<A> acquire, Function1<A, Par<B>> use, Consumer1<A> release) {
    return executor -> Future.bracket(acquire.apply(executor), a -> use.apply(a).apply(executor), release);
  }

  static <A, B, C> Par<C> map2(Par<A> parA, Par<B> parB, Function2<A, B, C> mapper) {
    return parB.ap(parA.map(mapper.curried()));
  }

  static <A, B> Par<Tuple2<A, B>> tuple(Par<A> parA, Par<B> parB) {
    return map2(parA, parB, Tuple::of);
  }

  static <A> Par<Sequence<A>> traverse(Sequence<Par<A>> sequence) {
    return sequence.foldLeft(success(empty()),
        (Par<Sequence<A>> xa, Par<A> a) -> map2(xa, a, Sequence::append));
  }

  static Par<Unit> sequence(Sequence<Par<?>> sequence) {
    return sequence.fold(unit(), Par::andThen).andThen(unit());
  }

  static Par<Unit> unit() {
    return success(Unit.unit());
  }
}

