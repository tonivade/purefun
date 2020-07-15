/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.effect;

import static com.github.tonivade.purefun.Function1.identity;
import static com.github.tonivade.purefun.Function2.first;
import static com.github.tonivade.purefun.Function2.second;
import static com.github.tonivade.purefun.Precondition.checkNonNull;
import java.time.Duration;
import java.util.concurrent.Executor;
import com.github.tonivade.purefun.CheckedRunnable;
import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Function2;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Witness;
import com.github.tonivade.purefun.Nothing;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Recoverable;
import com.github.tonivade.purefun.Tuple;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.concurrent.Future;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Try;
import com.github.tonivade.purefun.typeclasses.MonadDefer;

@HigherKind
public final class URIO<R, A> implements URIOOf<R, A>, Recoverable {

  private static final URIO<?, Unit> UNIT = new URIO<>(ZIO.unit());

  private final ZIO<R, Nothing, A> instance;

  URIO(ZIO<R, Nothing, A> value) {
    this.instance = checkNonNull(value);
  }

  public A unsafeRunSync(R env) {
    return instance.provide(env).get();
  }

  public Try<A> safeRunSync(R env) {
    return Try.of(() -> unsafeRunSync(env));
  }

  @SuppressWarnings("unchecked")
  public <E> ZIO<R, E, A> toZIO() {
    return (ZIO<R, E, A>) instance;
  }

  @SuppressWarnings("unchecked")
  public <E> EIO<E, A> toEIO() {
    return new EIO<>((ZIO<Nothing, E, A>) instance);
  }

  public RIO<R, A> toRIO() {
    return new RIO<>(ZIO.redeem(instance));
  }

  public Future<A> toFuture(R env) {
    return toFuture(Future.DEFAULT_EXECUTOR, env);
  }

  public Future<A> toFuture(Executor executor, R env) {
    return instance.toFuture(executor, env).map(Either::get);
  }

  public void safeRunAsync(Executor executor, R env, Consumer1<Try<A>> callback) {
    instance.provideAsync(executor, env, result -> callback.accept(result.map(Either::get)));
  }

  public void safeRunAsync(R env, Consumer1<Try<A>> callback) {
    safeRunAsync(Future.DEFAULT_EXECUTOR, env, callback);
  }

  public <F extends Witness> Kind<F, A> foldMap(R env, MonadDefer<F> monad) {
    return instance.foldMap(env, monad);
  }

  public <B> URIO<R, B> map(Function1<A, B> map) {
    return new URIO<>(instance.map(map));
  }

  public <B> URIO<R, B> flatMap(Function1<A, URIO<R, B>> map) {
    return new URIO<>(instance.flatMap(x -> map.apply(x).instance));
  }

  public <B> URIO<R, B> andThen(URIO<R, B> next) {
    return new URIO<>(instance.andThen(next.instance));
  }

  public URIO<R, A> recover(Function1<Throwable, A> mapError) {
    return redeem(mapError, identity());
  }

  @SuppressWarnings("unchecked")
  public <X extends Throwable> URIO<R, A> recoverWith(Class<X> type, Function1<X, A> function) {
    return recover(cause -> {
      if (type.isAssignableFrom(cause.getClass())) {
        return function.apply((X) cause);
      }
      return sneakyThrow(cause);
    });
  }

  public <B> URIO<R, B> redeem(Function1<Throwable, B> mapError, Function1<A, B> map) {
    return redeemWith(mapError.andThen(URIO::pure), map.andThen(URIO::pure));
  }

  public <B> URIO<R, B> redeemWith(Function1<Throwable, URIO<R, B>> mapError, Function1<A, URIO<R, B>> map) {
    return new URIO<>(ZIO.redeem(instance).foldM(error -> mapError.apply(error).instance, x -> map.apply(x).instance));
  }
  
  public <B> URIO<R, Tuple2<A, B>> zip(URIO<R, B> other) {
    return zipWith(other, Tuple::of);
  }
  
  public <B> URIO<R, A> zipLeft(URIO<R, B> other) {
    return zipWith(other, first());
  }
  
  public <B> URIO<R, B> zipRight(URIO<R, B> other) {
    return zipWith(other, second());
  }
  
  public <B, C> URIO<R, C> zipWith(URIO<R, B> other, Function2<A, B, C> mapper) {
    return map2(this, other, mapper);
  }

  public URIO<R, A> repeat() {
    return repeat(1);
  }

  public URIO<R, A> repeat(int times) {
    return fold(ZIO.redeem(instance).repeat(times));
  }

  public URIO<R, A> repeat(Duration delay) {
    return repeat(delay, 1);
  }

  public URIO<R, A> repeat(Duration delay, int times) {
    return fold(ZIO.redeem(instance).repeat(delay, times));
  }
  
  public <S, B> URIO<R, B> repeat(Schedule<R, S, A, B> schedule) {
    return fold(ZIO.redeem(instance).repeat(schedule));
  }

  public URIO<R, A> retry() {
    return retry(1);
  }

  public URIO<R, A> retry(int maxRetries) {
    return retry(Schedule.recurs(maxRetries));
  }

  public URIO<R, A> retry(Duration delay) {
    return retry(delay, 1);
  }

  public URIO<R, A> retry(Duration delay, int maxRetries) {
    return retry(Schedule.<R, Throwable>recursSpaced(delay, maxRetries));
  }
  
  public <S> URIO<R, A> retry(Schedule<R, S, Throwable, S> schedule) {
    return fold(ZIO.redeem(instance).retry(schedule));
  }

  public URIO<R, Tuple2<Duration, A>> timed() {
    return new URIO<>(instance.timed());
  }

  public static <R, A> URIO<R, A> accessM(Function1<R, URIO<R, A>> map) {
    return new URIO<>(ZIO.accessM(map.andThen(URIO::toZIO)));
  }

  public static <R, A> URIO<R, A> access(Function1<R, A> map) {
    return accessM(map.andThen(URIO::pure));
  }

  public static <R> URIO<R, R> env() {
    return access(identity());
  }

  public static <R, A, B, C> URIO<R, C> map2(URIO<R, A> za, URIO<R, B> zb, Function2<A, B, C> mapper) {
    return new URIO<>(ZIO.map2(za.instance, zb.instance, mapper));
  }

  public static <R, A, B> Function1<A, URIO<R, B>> lift(Function1<A, B> function) {
    return value -> task(() -> function.apply(value));
  }

  public static <R> URIO<R, Unit> sleep(Duration delay) {
    return fold(ZIO.sleep(delay));
  }

  public static <R> URIO<R, Unit> exec(CheckedRunnable task) {
    return fold(ZIO.exec(task));
  }

  public static <R, A> URIO<R, A> pure(A value) {
    return new URIO<>(ZIO.pure(value));
  }

  public static <R, A> URIO<R, A> raiseError(Throwable throwable) {
    return new URIO<>(ZIO.fromEither(() -> { throw throwable; }));
  }

  public static <R, A> URIO<R, A> defer(Producer<URIO<R, A>> lazy) {
    return new URIO<>(ZIO.defer(() -> lazy.get().instance));
  }

  public static <R, A> URIO<R, A> task(Producer<A> task) {
    return fold(ZIO.task(task));
  }

  public static <R, A extends AutoCloseable, B> URIO<R, B> bracket(URIO<R, A> acquire, Function1<A, URIO<R, B>> use) {
    return fold(ZIO.bracket(ZIO.redeem(acquire.instance), resource -> ZIO.redeem(use.apply(resource).instance)));
  }

  public static <R, A, B> URIO<R, B> bracket(URIO<R, A> acquire, Function1<A, URIO<R, B>> use, Consumer1<A> release) {
    return fold(ZIO.bracket(ZIO.redeem(acquire.instance), resource -> ZIO.redeem(use.apply(resource).instance), release));
  }

  @SuppressWarnings("unchecked")
  public static <R> URIO<R, Unit> unit() {
    return (URIO<R, Unit>) UNIT;
  }

  private static <R, A> URIO<R, A> fold(ZIO<R, Throwable, A> zio) {
    return new URIO<>(zio.foldM(error -> URIO.<R, A>raiseError(error).instance, value -> URIO.<R, A>pure(value).instance));
  }
}
