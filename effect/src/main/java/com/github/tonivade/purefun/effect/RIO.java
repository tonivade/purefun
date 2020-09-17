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
public final class RIO<R, A> implements RIOOf<R, A>, Recoverable {

  private static final RIO<?, Unit> UNIT = new RIO<>(ZIO.unit());

  private final ZIO<R, Throwable, A> instance;

  RIO(ZIO<R, Throwable, A> value) {
    this.instance = checkNonNull(value);
  }

  public Try<A> safeRunSync(R env) {
    return Try.fromEither(instance.provide(env));
  }

  public ZIO<R, Throwable, A> toZIO() {
    return instance;
  }

  @SuppressWarnings("unchecked")
  public <E> EIO<E, A> toEIO() {
    return new EIO<>((ZIO<Nothing, E, A>) instance);
  }
  
  public URIO<R, A> toURIO() {
    return recover(this::sneakyThrow);
  }

  public Future<Try<A>> toFuture(R env) {
    return toFuture(Future.DEFAULT_EXECUTOR, env);
  }

  public Future<Try<A>> toFuture(Executor executor, R env) {
    return instance.toFuture(executor, env).map(Try::fromEither);
  }

  public void safeRunAsync(Executor executor, R env, Consumer1<Try<A>> callback) {
    instance.provideAsync(executor, env, result -> callback.accept(flatAbsorb(result)));
  }

  public void safeRunAsync(R env, Consumer1<Try<A>> callback) {
    safeRunAsync(Future.DEFAULT_EXECUTOR, env, callback);
  }

  public <F extends Witness> Kind<F, A> foldMap(R env, Async<F> monad) {
    return instance.foldMap(env, monad);
  }

  public <B> RIO<R, B> map(Function1<A, B> map) {
    return new RIO<>(instance.map(map));
  }

  public <B> RIO<R, B> flatMap(Function1<A, RIO<R, B>> map) {
    return new RIO<>(instance.flatMap(x -> map.apply(x).instance));
  }

  public <B> RIO<R, B> andThen(RIO<R, B> next) {
    return new RIO<>(instance.andThen(next.instance));
  }

  public <B> RIO<R, B> ap(RIO<R, Function1<A, B>> apply) {
    return new RIO<>(instance.ap(apply.toZIO()));
  }

  public URIO<R, A> recover(Function1<Throwable, A> mapError) {
    return fold(mapError, identity());
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

  public <B> URIO<R, B> fold(Function1<Throwable, B> mapError, Function1<A, B> map) {
    return new URIO<>(instance.foldM(mapError.andThen(ZIO::pure), map.andThen(ZIO::pure)));
  }

  public <B> RIO<R, B> foldM(Function1<Throwable, RIO<R, B>> mapError, Function1<A, RIO<R, B>> map) {
    return new RIO<>(instance.foldM(error -> mapError.apply(error).instance, x -> map.apply(x).instance));
  }

  public RIO<R, A> orElse(RIO<R, A> other) {
    return foldM(Function1.cons(other), Function1.cons(this));
  }
  
  public <B> RIO<R, Tuple2<A, B>> zip(RIO<R, B> other) {
    return zipWith(other, Tuple::of);
  }
  
  public <B> RIO<R, A> zipLeft(RIO<R, B> other) {
    return zipWith(other, first());
  }
  
  public <B> RIO<R, B> zipRight(RIO<R, B> other) {
    return zipWith(other, second());
  }
  
  public <B, C> RIO<R, C> zipWith(RIO<R, B> other, Function2<A, B, C> mapper) {
    return map2(this, other, mapper);
  }

  public RIO<R, A> repeat() {
    return repeat(1);
  }

  public RIO<R, A> repeat(int times) {
    return new RIO<>(instance.repeat(times));
  }

  public RIO<R, A> repeat(Duration delay) {
    return repeat(delay, 1);
  }

  public RIO<R, A> repeat(Duration delay, int times) {
    return new RIO<>(instance.repeat(delay, times));
  }
  
  public <B> RIO<R, B> repeat(Schedule<R, A, B> schedule) {
    return new RIO<>(instance.repeat(schedule));
  }

  public RIO<R, A> retry() {
    return retry(1);
  }

  public RIO<R, A> retry(int maxRetries) {
    return retry(Schedule.recurs(maxRetries));
  }

  public RIO<R, A> retry(Duration delay) {
    return retry(delay, 1);
  }

  public RIO<R, A> retry(Duration delay, int maxRetries) {
    return retry(Schedule.<R, Throwable>recursSpaced(delay, maxRetries));
  }
  
  public <B> RIO<R, A> retry(Schedule<R, Throwable, B> schedule) {
    return new RIO<>(instance.retry(schedule));
  }

  public RIO<R, Tuple2<Duration, A>> timed() {
    return new RIO<>(instance.timed());
  }

  public static <R, A> RIO<R, A> accessM(Function1<R, RIO<R, A>> map) {
    return new RIO<>(ZIO.accessM(map.andThen(RIO::toZIO)));
  }

  public static <R, A> RIO<R, A> access(Function1<R, A> map) {
    return accessM(map.andThen(RIO::pure));
  }

  public static <R> RIO<R, R> env() {
    return access(identity());
  }

  public static <R, A> RIO<R, A> absorb(RIO<R, Either<Throwable, A>> value) {
    return new RIO<>(ZIO.absorb(value.instance));
  }

  public static <R, A, B, C> RIO<R, C> map2(RIO<R, A> za, RIO<R, B> zb, Function2<A, B, C> mapper) {
    return new RIO<>(ZIO.map2(za.instance, zb.instance, mapper));
  }

  public static <R> RIO<R, Unit> sleep(Duration delay) {
    return new RIO<>(ZIO.sleep(delay));
  }

  public static <R, A, B> Function1<A, RIO<R, B>> lift(Function1<A, B> function) {
    return value -> task(() -> function.apply(value));
  }

  public static <R, A> RIO<R, A> fromEither(Producer<Either<Throwable, A>> task) {
    return new RIO<>(ZIO.fromEither(task));
  }

  public static <R> RIO<R, Unit> exec(CheckedRunnable task) {
    return new RIO<>(ZIO.exec(task));
  }

  public static <R, A> RIO<R, A> pure(A value) {
    return new RIO<>(ZIO.pure(value));
  }

  public static <R, A> RIO<R, A> raiseError(Throwable throwable) {
    return new RIO<>(ZIO.raiseError(throwable));
  }

  public static <R, A> RIO<R, A> defer(Producer<RIO<R, A>> lazy) {
    return new RIO<>(ZIO.defer(() -> lazy.get().instance));
  }

  public static <R, A> RIO<R, A> task(Producer<A> task) {
    return new RIO<>(ZIO.task(task));
  }
  
  public static <R, A> RIO<R, A> async(Consumer1<Consumer1<Try<A>>> consumer) {
    return new RIO<>(ZIO.async(consumer));
  }

  public static <R, A extends AutoCloseable, B> RIO<R, B> bracket(RIO<R, A> acquire, Function1<A, RIO<R, B>> use) {
    return new RIO<>(ZIO.bracket(acquire.instance, resource -> use.apply(resource).instance));
  }

  public static <R, A, B> RIO<R, B> bracket(RIO<R, A> acquire, Function1<A, RIO<R, B>> use, Consumer1<A> release) {
    return new RIO<>(ZIO.bracket(acquire.instance, resource -> use.apply(resource).instance, release));
  }

  @SuppressWarnings("unchecked")
  public static <R> RIO<R, Unit> unit() {
    return (RIO<R, Unit>) UNIT;
  }

  private Try<A> flatAbsorb(Try<Either<Throwable, A>> result) {
    return result.map(Try::fromEither).flatMap(identity());
  }
}
