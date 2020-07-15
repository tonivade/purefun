/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.effect;

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
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Witness;
import com.github.tonivade.purefun.Nothing;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Tuple;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.concurrent.Future;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Try;
import com.github.tonivade.purefun.typeclasses.MonadDefer;

@HigherKind
public final class EIO<E, A> implements EIOOf<E, A> {

  private static final EIO<?, Unit> UNIT = new EIO<>(ZIO.unit());

  private final ZIO<Nothing, E, A> instance;

  EIO(ZIO<Nothing, E, A> value) {
    this.instance = checkNonNull(value);
  }

  @SuppressWarnings("unchecked")
  public <R> ZIO<R, E, A> toZIO() {
    return (ZIO<R, E, A>) instance;
  }

  public Either<E, A> safeRunSync() {
    return instance.provide(nothing());
  }

  public Future<Either<E, A>> toFuture() {
    return toFuture(Future.DEFAULT_EXECUTOR);
  }

  public Future<Either<E, A>> toFuture(Executor executor) {
    return instance.toFuture(executor, nothing());
  }

  public void safeRunAsync(Executor executor, Consumer1<Try<Either<E, A>>> callback) {
    instance.provideAsync(executor, nothing(), callback);
  }

  public void safeRunAsync(Consumer1<Try<Either<E, A>>> callback) {
    safeRunAsync(Future.DEFAULT_EXECUTOR, callback);
  }

  public <F extends Witness> Kind<F, A> foldMap(MonadDefer<F> monad) {
    return instance.foldMap(nothing(), monad);
  }

  public <B> EIO<E, B> map(Function1<A, B> map) {
    return new EIO<>(instance.map(map));
  }

  public <B> EIO<E, B> flatMap(Function1<A, EIO<E, B>> map) {
    return new EIO<>(instance.flatMap(value -> map.apply(value).instance));
  }

  public EIO<A, E> swap() {
    return new EIO<>(instance.swap());
  }

  public <B> EIO<B, A> mapError(Function1<E, B> map) {
    return new EIO<>(instance.mapError(map));
  }

  public <F> EIO<F, A> flatMapError(Function1<E, EIO<F, A>> map) {
    return new EIO<>(instance.flatMapError(error -> map.apply(error).instance));
  }

  public <B, F> EIO<F, B> bimap(Function1<E, F> mapError, Function1<A, B> map) {
    return new EIO<>(instance.bimap(mapError, map));
  }

  public <B> EIO<E, B> andThen(EIO<E, B> next) {
    return new EIO<>(instance.andThen(next.instance));
  }

  public <B, F> EIO<F, B> foldM(Function1<E, EIO<F, B>> mapError, Function1<A, EIO<F, B>> map) {
    return new EIO<>(instance.foldM(error -> mapError.apply(error).instance, value -> map.apply(value).instance));
  }

  public <B> UIO<B> fold(Function1<E, B> mapError, Function1<A, B> map) {
    return new UIO<>(instance.fold(mapError, map));
  }

  public UIO<A> recover(Function1<E, A> mapError) {
    return new UIO<>(instance.recover(mapError));
  }

  public EIO<E, A> orElse(EIO<E, A> other) {
    return new EIO<>(instance.orElse(other.instance));
  }
  
  public <B> EIO<E, Tuple2<A, B>> zip(EIO<E, B> other) {
    return zipWith(other, Tuple::of);
  }
  
  public <B> EIO<E, A> zipLeft(EIO<E, B> other) {
    return zipWith(other, first());
  }
  
  public <B> EIO<E, B> zipRight(EIO<E, B> other) {
    return zipWith(other, second());
  }
  
  public <B, C> EIO<E, C> zipWith(EIO<E, B> other, Function2<A, B, C> mapper) {
    return map2(this, other, mapper);
  }

  public EIO<E, A> repeat() {
    return repeat(1);
  }

  public EIO<E, A> repeat(int times) {
    return new EIO<>(instance.repeat(times));
  }

  public EIO<E, A> repeat(Duration delay) {
    return repeat(delay, 1);
  }

  public EIO<E, A> repeat(Duration delay, int times) {
    return new EIO<>(instance.repeat(delay, times));
  }
  
  public <S, B> EIO<E, B> repeat(Schedule<Nothing, S, A, B> schedule) {
    return new EIO<>(instance.repeat(schedule));
  }

  public EIO<E, A> retry() {
    return retry(1);
  }

  public EIO<E, A> retry(int maxRetries) {
    return retry(Schedule.recurs(maxRetries));
  }

  public EIO<E, A> retry(Duration delay) {
    return retry(delay, 1);
  }

  public EIO<E, A> retry(Duration delay, int maxRetries) {
    return retry(Schedule.<Nothing, E>recursSpaced(delay, maxRetries));
  }
  
  public <S> EIO<E, A> retry(Schedule<Nothing, S, E, S> schedule) {
    return new EIO<>(instance.retry(schedule));
  }

  public EIO<E, Tuple2<Duration, A>> timed() {
    return new EIO<>(instance.timed());
  }
  
  public <X extends Throwable> EIO<X, A> refineOrDie(Class<X> type) {
    return new EIO<>(instance.refineOrDie(type));
  }
  
  public UIO<A> orDie() {
    return new UIO<>(instance.orDie().toZIO());
  }

  public static <E, A, B, C> EIO<E, C> map2(EIO<E, A> za, EIO<E, B> zb, Function2<A, B, C> mapper) {
    return new EIO<>(ZIO.map2(za.instance, zb.instance, mapper));
  }

  public static <E, A> EIO<E, A> absorb(EIO<E, Either<E, A>> value) {
    return new EIO<>(ZIO.absorb(value.instance));
  }

  public static <A, B> Function1<A, EIO<Throwable, B>> lift(Function1<A, B> function) {
    return ZIO.<Nothing, A, B>lift(function).andThen(EIO::new);
  }

  public static <E, A> EIO<E, A> fromEither(Producer<Either<E, A>> task) {
    return new EIO<>(ZIO.fromEither(task));
  }

  public static EIO<Throwable, Unit> exec(CheckedRunnable task) {
    return new EIO<>(ZIO.exec(task));
  }

  public static <E, A> EIO<E, A> pure(A value) {
    return new EIO<>(ZIO.pure(value));
  }

  public static <E, A> EIO<E, A> defer(Producer<EIO<E, A>> lazy) {
    return new EIO<>(ZIO.defer(() -> lazy.get().instance));
  }

  public static <A> EIO<Throwable, A> task(Producer<A> task) {
    return new EIO<>(ZIO.task(task));
  }

  public static <E, A> EIO<E, A> raiseError(E error) {
    return new EIO<>(ZIO.raiseError(error));
  }

  public static <E, A extends AutoCloseable, B> EIO<E, B> bracket(EIO<E, A> acquire, Function1<A, EIO<E, B>> use) {
    return new EIO<>(ZIO.bracket(acquire.instance, resource -> use.apply(resource).instance));
  }

  public static <E, A, B> EIO<E, B> bracket(EIO<E, A> acquire, Function1<A, EIO<E, B>> use, Consumer1<A> release) {
    return new EIO<>(ZIO.bracket(acquire.instance, resource -> use.apply(resource).instance, release));
  }

  @SuppressWarnings("unchecked")
  public static <E> EIO<E, Unit> unit() {
    return (EIO<E, Unit>) UNIT;
  }
}
