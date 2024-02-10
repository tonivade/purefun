/*
 * Copyright (c) 2018-2023, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
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
import com.github.tonivade.purefun.Bindable;
import com.github.tonivade.purefun.Matcher1;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Tuple;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.annotation.HigherKind;
import com.github.tonivade.purefun.annotation.Kind;
import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.type.Try;

@HigherKind
@FunctionalInterface
public non-sealed interface Par<T> extends ParOf<T>, Bindable<Par_, T> {

  Future<T> apply(Executor executor);

  default Promise<T> run(Executor executor) {
    return apply(executor).toPromise();
  }

  default Promise<T> run() {
    return run(Future.DEFAULT_EXECUTOR);
  }

  @Override
  default <R> Par<R> map(Function1<? super T, ? extends R> mapper) {
    return executor -> apply(executor).map(mapper);
  }

  @Override
  default <R> Par<R> flatMap(Function1<? super T, ? extends Kind<Par_, ? extends R>> mapper) {
    return executor -> apply(executor).flatMap(value -> mapper.andThen(ParOf::narrowK).apply(value).apply(executor));
  }

  default <R> Par<R> andThen(Par<? extends R> next) {
    return map2(this, next, second());
  }

  default Par<T> onSuccess(Consumer1<? super T> callback) {
    return executor -> apply(executor).onSuccess(callback);
  }

  default Par<T> onFailure(Consumer1<? super Throwable> callback) {
    return executor -> apply(executor).onFailure(callback);
  }

  default Par<T> onComplete(Consumer1<? super Try<? extends T>> callback) {
    return executor -> apply(executor).onComplete(callback);
  }

  default <R> Par<R> ap(Par<Function1<? super T, ? extends R>> apply) {
    return executor -> apply(executor).ap(apply.apply(executor));
  }

  default Par<T> filter(Matcher1<? super T> matcher) {
    return executor -> apply(executor).filter(matcher);
  }

  default Par<T> filterNot(Matcher1<? super T> matcher) {
    return executor -> apply(executor).filterNot(matcher);
  }

  default Par<T> recover(Function1<? super Throwable, ? extends T> recover) {
    return fold(recover, identity());
  }

  default <X extends Throwable> Par<T> recoverWith(Class<X> type, 
      Function1<? super X, ? extends T> mapper) {
    return executor -> apply(executor).recoverWith(type, mapper);
  }

  default <R> Par<R> fold(Function1<? super Throwable, ? extends R> failureMapper, 
      Function1<? super T, ? extends R> successMapper) {
    return executor -> apply(executor).fold(failureMapper, successMapper);
  }

  static <T> Par<T> success(T value) {
    return executor -> Future.success(executor, value);
  }

  static <T> Par<T> failure(Throwable error) {
    return executor -> Future.failure(executor, error);
  }

  static <T> Par<T> task(Producer<? extends T> producer) {
    return executor -> Future.task(executor, producer);
  }

  static <T> Par<T> defer(Producer<? extends Kind<Par_, ? extends T>> producer) {
    return executor -> {
      Producer<Par<T>> andThen = producer.andThen(ParOf::narrowK);
      return andThen.get().apply(executor);
    };
  }

  static <T> Par<T> later(Producer<? extends T> producer) {
    return executor -> Future.later(executor, producer::get);
  }

  static <T> Par<T> delay(Duration delay, Producer<? extends T> producer) {
    return executor -> Future.delay(executor, delay, producer::get);
  }

  static Par<Unit> exec(CheckedRunnable runnable) {
    return executor -> Future.exec(executor, runnable);
  }

  static <T> Par<T> async(Consumer1<Consumer1<? super Try<? extends T>>> consumer) {
    return executor -> Future.async(executor, consumer);
  }

  static Par<Unit> sleep(Duration delay) {
    return executor -> Future.sleep(executor, delay);
  }

  static <A, B> Par<B> bracket(Par<? extends A> acquire, 
      Function1<? super A, ? extends Par<? extends B>> use, Consumer1<? super A> release) {
    return executor -> Future.bracket(acquire.apply(executor), a -> use.apply(a).apply(executor), release);
  }

  static <A, B, C> Par<C> map2(Par<? extends A> parA, Par<? extends B> parB, 
      Function2<? super A, ? super B, ? extends C> mapper) {
    return parB.ap(parA.map(mapper.curried()));
  }

  static <A, B> Par<Tuple2<A, B>> tuple(Par<? extends A> parA, Par<? extends B> parB) {
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

