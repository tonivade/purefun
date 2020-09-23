/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.effect;

import static com.github.tonivade.purefun.Function1.identity;
import static com.github.tonivade.purefun.Function2.first;
import static com.github.tonivade.purefun.Function2.second;
import static com.github.tonivade.purefun.Nothing.nothing;
import static com.github.tonivade.purefun.Precondition.checkNonNull;

import java.time.Duration;
import java.util.concurrent.Executor;

import com.github.tonivade.purefun.CheckedRunnable;
import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Function2;
import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Nothing;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Recoverable;
import com.github.tonivade.purefun.Tuple;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.Witness;
import com.github.tonivade.purefun.concurrent.Future;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Try;
import com.github.tonivade.purefun.typeclasses.Async;

@HigherKind
public final class UIO<A> implements UIOOf<A>, Recoverable {

  private static final UIO<Unit> UNIT = new UIO<>(ZIO.unit());

  private final ZIO<Nothing, Nothing, A> instance;

  UIO(ZIO<Nothing, Nothing, A> value) {
    this.instance = checkNonNull(value);
  }

  public A unsafeRunSync() {
    return instance.provide(nothing()).get();
  }

  public Try<A> safeRunSync() {
    return Try.of(this::unsafeRunSync);
  }

  @SuppressWarnings("unchecked")
  public <R, E> ZIO<R, E, A> toZIO() {
    return (ZIO<R, E, A>) instance;
  }

  @SuppressWarnings("unchecked")
  public <E> EIO<E, A> toEIO() {
    return new EIO<>((ZIO<Nothing, E, A>) instance);
  }

  @SuppressWarnings("unchecked")
  public <R> RIO<R, A> toRIO() {
    return new RIO<>((ZIO<R, Throwable, A>) ZIO.redeem(instance));
  }

  @SuppressWarnings("unchecked")
  public <R> URIO<R, A> toURIO() {
    return new URIO<>((ZIO<R, Nothing, A>) instance);
  }

  public Task<A> toTask() {
    return new Task<>(ZIO.redeem(instance));
  }

  public Future<A> toFuture() {
    return toFuture(Future.DEFAULT_EXECUTOR);
  }

  public Future<A> toFuture(Executor executor) {
    return instance.toFuture(executor, nothing()).map(Either::get);
  }

  public void safeRunAsync(Executor executor, Consumer1<Try<A>> callback) {
    instance.provideAsync(executor, nothing(), result -> callback.accept(result.map(Either::get)));
  }

  public void safeRunAsync(Consumer1<Try<A>> callback) {
    safeRunAsync(Future.DEFAULT_EXECUTOR, callback);
  }

  public <F extends Witness> Kind<F, A> foldMap(Async<F> monad) {
    return instance.foldMap(nothing(), monad);
  }

  public <B> UIO<B> map(Function1<? super A, ? extends B> map) {
    return new UIO<>(instance.map(map));
  }

  public <B> UIO<B> flatMap(Function1<? super A, ? extends UIO<? extends B>> map) {
    return new UIO<>(instance.flatMap(x -> {
      UIO<? extends B> apply = map.apply(x);
      return apply.instance;
    }));
  }

  public <B> UIO<B> andThen(UIO<B> next) {
    return new UIO<>(instance.andThen(next.instance));
  }

  public <B> UIO<B> ap(UIO<Function1<? super A, ? extends B>> apply) {
    return new UIO<>(instance.ap(apply.toZIO()));
  }

  public UIO<A> recover(Function1<? super Throwable, ? extends A> mapError) {
    return redeem(mapError, identity());
  }

  @SuppressWarnings("unchecked")
  public <X extends Throwable> UIO<A> recoverWith(Class<X> type, Function1<? super X, ? extends A> function) {
    return recover(cause -> {
      if (type.isAssignableFrom(cause.getClass())) {
        return function.apply((X) cause);
      }
      return sneakyThrow(cause);
    });
  }

  public <B> UIO<B> redeem(Function1<? super Throwable, ? extends B> mapError, Function1<? super A, ? extends B> map) {
    return redeemWith(mapError.andThen(UIO::pure), map.andThen(UIO::pure));
  }

  public <B> UIO<B> redeemWith(
      Function1<? super Throwable, ? extends UIO<? extends B>> mapError, 
      Function1<? super A, ? extends UIO<? extends B>> map) {
    return new UIO<>(ZIO.redeem(instance).foldM(
        error -> mapError.andThen(UIOOf::narrowK).apply(error).instance, 
        value -> map.andThen(UIOOf::narrowK).apply(value).instance));
  }
  
  public <B> UIO<Tuple2<A, B>> zip(UIO<B> other) {
    return zipWith(other, Tuple::of);
  }
  
  public <B> UIO<A> zipLeft(UIO<B> other) {
    return zipWith(other, first());
  }
  
  public <B> UIO<B> zipRight(UIO<B> other) {
    return zipWith(other, second());
  }
  
  public <B, C> UIO<C> zipWith(UIO<B> other, Function2<A, B, C> mapper) {
    return map2(this, other, mapper);
  }

  public UIO<A> repeat() {
    return repeat(1);
  }

  public UIO<A> repeat(int times) {
    return fold(ZIO.redeem(instance).repeat(times));
  }

  public UIO<A> repeat(Duration delay) {
    return repeat(delay, 1);
  }

  public UIO<A> repeat(Duration delay, int times) {
    return fold(ZIO.redeem(instance).repeat(delay, times));
  }
  
  public <B> UIO<B> repeat(Schedule<Nothing, A, B> schedule) {
    return fold(ZIO.redeem(instance).repeat(schedule));
  }

  public UIO<A> retry() {
    return retry(1);
  }

  public UIO<A> retry(int maxRetries) {
    return retry(Schedule.recurs(maxRetries));
  }

  public UIO<A> retry(Duration delay) {
    return retry(delay, 1);
  }

  public UIO<A> retry(Duration delay, int maxRetries) {
    return retry(Schedule.<Nothing, Throwable>recursSpaced(delay, maxRetries));
  }
  
  public <B> UIO<A> retry(Schedule<Nothing, Throwable, B> schedule) {
    return fold(ZIO.redeem(instance).retry(schedule));
  }

  public UIO<Tuple2<Duration, A>> timed() {
    return new UIO<>(instance.timed());
  }

  public static <A, B, C> UIO<C> map2(UIO<A> za, UIO<B> zb, Function2<A, B, C> mapper) {
    return new UIO<>(ZIO.map2(za.instance, zb.instance, mapper));
  }

  public static <A, B> Function1<A, UIO<B>> lift(Function1<A, B> function) {
    return value -> task(() -> function.apply(value));
  }

  public static UIO<Unit> sleep(Duration delay) {
    return fold(ZIO.sleep(delay));
  }

  public static UIO<Unit> exec(CheckedRunnable task) {
    return fold(ZIO.exec(task));
  }

  public static <A> UIO<A> pure(A value) {
    return new UIO<>(ZIO.pure(value));
  }

  public static <A> UIO<A> raiseError(Throwable throwable) {
    return new UIO<>(ZIO.fromEither(() -> { throw throwable; }));
  }

  public static <A> UIO<A> defer(Producer<UIO<A>> lazy) {
    return new UIO<>(ZIO.defer(() -> lazy.get().instance));
  }

  public static <A> UIO<A> task(Producer<A> task) {
    return fold(ZIO.task(task));
  }
  
  public static <A> UIO<A> async(Consumer1<Consumer1<? super Try<? extends A>>> consumer) {
    return fold(ZIO.async(consumer));
  }

  public static <A extends AutoCloseable, B> UIO<B> bracket(
      UIO<? extends A> acquire, Function1<? super A, ? extends UIO<? extends B>> use) {
    return fold(ZIO.bracket(ZIO.redeem(acquire.instance), 
        resource -> ZIO.redeem(use.andThen(UIOOf::narrowK).apply(resource).instance)));
  }

  public static <A, B> UIO<B> bracket(UIO<? extends A> acquire, 
      Function1<? super A, ? extends UIO<? extends B>> use, Consumer1<? super A> release) {
    return fold(ZIO.bracket(ZIO.redeem(acquire.instance), 
        resource -> ZIO.redeem(use.andThen(UIOOf::narrowK).apply(resource).instance), release));
  }

  public static UIO<Unit> unit() {
    return UNIT;
  }

  private static <A> UIO<A> fold(ZIO<Nothing, Throwable, A> zio) {
    return new UIO<>(zio.foldM(error -> UIO.<A>raiseError(error).instance, value -> UIO.pure(value).instance));
  }
}
