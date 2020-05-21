/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.effect;

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
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.concurrent.Future;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Try;
import com.github.tonivade.purefun.typeclasses.MonadDefer;

@HigherKind
public final class EIO<E, T> implements EIOOf<E, T> {

  private final ZIO<Nothing, E, T> value;

  EIO(ZIO<Nothing, E, T> value) {
    this.value = checkNonNull(value);
  }

  @SuppressWarnings("unchecked")
  public <R> ZIO<R, E, T> toZIO() {
    return (ZIO<R, E, T>) value;
  }

  public Either<E, T> safeRunSync() {
    return value.provide(nothing());
  }

  public Future<Either<E, T>> toFuture() {
    return value.toFuture(nothing());
  }

  public void async(Executor executor, Consumer1<Try<Either<E, T>>> callback) {
    value.provideAsync(executor, nothing(), callback);
  }

  public void async(Consumer1<Try<Either<E, T>>> callback) {
    async(Future.DEFAULT_EXECUTOR, callback);
  }

  public <F extends Witness> Kind<F, Either<E, T>> foldMap(MonadDefer<F> monad) {
    return value.foldMap(nothing(), monad);
  }

  public <B> EIO<E, B> map(Function1<T, B> map) {
    return new EIO<>(value.map(map));
  }

  public <B> EIO<E, B> flatMap(Function1<T, EIO<E, B>> map) {
    return new EIO<>(value.flatMap(value -> map.apply(value).value));
  }

  public EIO<T, E> swap() {
    return new EIO<>(value.swap());
  }

  public <B> EIO<B, T> mapError(Function1<E, B> map) {
    return new EIO<>(value.mapError(map));
  }

  public <F> EIO<F, T> flatMapError(Function1<E, EIO<F, T>> map) {
    return new EIO<>(value.flatMapError(error -> map.apply(error).value));
  }

  public <B, F> EIO<F, B> bimap(Function1<E, F> mapError, Function1<T, B> map) {
    return new EIO<>(value.bimap(mapError, map));
  }

  public <B> EIO<E, B> andThen(EIO<E, B> next) {
    return new EIO<>(value.andThen(next.value));
  }

  public <B, F> EIO<F, B> foldM(Function1<E, EIO<F, B>> mapError, Function1<T, EIO<F, B>> map) {
    return new EIO<>(value.foldM(error -> mapError.apply(error).value, value -> map.apply(value).value));
  }

  public <B> UIO<B> fold(Function1<E, B> mapError, Function1<T, B> map) {
    return new UIO<>(value.fold(mapError, map));
  }

  public UIO<T> recover(Function1<E, T> mapError) {
    return new UIO<>(value.recover(mapError));
  }

  public EIO<E, T> orElse(Producer<EIO<E, T>> other) {
    return new EIO<>(value.orElse(() -> other.get().value));
  }

  public EIO<E, T> repeat() {
    return repeat(1);
  }

  public EIO<E, T> repeat(int times) {
    return repeat(UIO.unit(), times);
  }

  public EIO<E, T> repeat(Duration delay) {
    return repeat(delay, 1);
  }

  public EIO<E, T> repeat(Duration delay, int times) {
    return repeat(UIO.sleep(delay), times);
  }

  public EIO<E, T> retry() {
    return retry(1);
  }

  public EIO<E, T> retry(int maxRetries) {
    return retry(UIO.unit(), maxRetries);
  }

  public EIO<E, T> retry(Duration delay) {
    return retry(delay, 1);
  }

  public EIO<E, T> retry(Duration delay, int maxRetries) {
    return retry(UIO.sleep(delay), maxRetries);
  }

  private EIO<E, T> repeat(UIO<Unit> pause, int times) {
    return foldM(EIO::raiseError, value -> {
      if (times > 0) {
        return pause.<E>toEIO().andThen(repeat(pause, times - 1));
      } else {
        return pure(value);
      }
    });
  }

  private EIO<E, T> retry(UIO<Unit> pause, int maxRetries) {
    return foldM(error -> {
      if (maxRetries > 0) {
        return pause.<E>toEIO().andThen(retry(pause.repeat(), maxRetries - 1));
      } else {
        return raiseError(error);
      }
    }, EIO::pure);
  }

  public static <E, A, B, C> EIO<E, C> map2(EIO<E, A> za, EIO<E, B> zb, Function2<A, B, C> mapper) {
    return new EIO<>(ZIO.map2(za.value, zb.value, mapper));
  }

  public static <E, A> EIO<E, A> absorb(EIO<E, Either<E, A>> value) {
    return new EIO<>(ZIO.absorb(value.value));
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
    return new EIO<>(ZIO.defer(() -> lazy.get().value));
  }

  public static <A> EIO<Throwable, A> task(Producer<A> task) {
    return new EIO<>(ZIO.task(task));
  }

  public static <E, A> EIO<E, A> raiseError(E error) {
    return new EIO<>(ZIO.raiseError(error));
  }

  public static <A extends AutoCloseable, B> EIO<Throwable, B> bracket(EIO<Throwable, A> acquire, Function1<A, EIO<Throwable, B>> use) {
    return new EIO<>(ZIO.bracket(acquire.value, resource -> use.apply(resource).value));
  }

  public static <A, B> EIO<Throwable, B> bracket(EIO<Throwable, A> acquire, Function1<A, EIO<Throwable, B>> use, Consumer1<A> release) {
    return new EIO<>(ZIO.bracket(acquire.value, resource -> use.apply(resource).value, release));
  }

  public static <E> EIO<E, Unit> unit() {
    return new EIO<>(ZIO.unit());
  }
}
