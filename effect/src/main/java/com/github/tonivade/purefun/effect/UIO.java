/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.effect;

import static com.github.tonivade.purefun.Function1.identity;
import static com.github.tonivade.purefun.Nothing.nothing;
import static java.util.Objects.requireNonNull;

import java.time.Duration;
import java.util.concurrent.Executor;

import com.github.tonivade.purefun.CheckedRunnable;
import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Function2;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Nothing;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Recoverable;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.concurrent.Future;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Try;
import com.github.tonivade.purefun.typeclasses.MonadDefer;

@HigherKind
public final class UIO<T> implements Recoverable {

  private final ZIO<Nothing, Nothing, T> value;

  UIO(ZIO<Nothing, Nothing, T> value) {
    this.value = requireNonNull(value);
  }

  public T unsafeRunSync() {
    return value.provide(nothing()).get();
  }

  public Try<T> safeRunSync() {
    return Try.of(this::unsafeRunSync);
  }

  @SuppressWarnings("unchecked")
  public <R, E> ZIO<R, E, T> toZIO() {
    return (ZIO<R, E, T>) value;
  }

  @SuppressWarnings("unchecked")
  public <E> EIO<E, T> toEIO() {
    return new EIO<>((ZIO<Nothing, E, T>) value);
  }

  public Future<T> toFuture() {
    return value.toFuture(nothing()).map(Either::get);
  }

  public void async(Executor executor, Consumer1<Try<T>> callback) {
    value.provideAsync(executor, nothing(), result -> callback.accept(result.map(Either::get)));
  }

  public void async(Consumer1<Try<T>> callback) {
    async(Future.DEFAULT_EXECUTOR, callback);
  }

  public <F extends Kind> Higher1<F, T> foldMap(MonadDefer<F> monad) {
    return monad.map(value.foldMap(nothing(), monad), Either::get);
  }

  public <B> UIO<B> map(Function1<T, B> map) {
    return new UIO<>(value.map(map));
  }

  public <B> UIO<B> flatMap(Function1<T, UIO<B>> map) {
    return new UIO<>(value.flatMap(x -> map.apply(x).value));
  }

  public <B> UIO<B> andThen(UIO<B> next) {
    return new UIO<>(value.andThen(next.value));
  }

  public UIO<T> recover(Function1<Throwable, T> mapError) {
    return redeem(mapError, identity());
  }

  @SuppressWarnings("unchecked")
  public <X extends Throwable> UIO<T> recoverWith(Class<X> type, Function1<X, T> function) {
    return recover(cause -> {
      if (type.isAssignableFrom(cause.getClass())) {
        return function.apply((X) cause);
      }
      return sneakyThrow(cause);
    });
  }

  public <B> UIO<B> redeem(Function1<Throwable, B> mapError, Function1<T, B> map) {
    return redeemWith(mapError.andThen(UIO::pure), map.andThen(UIO::pure));
  }

  public <B> UIO<B> redeemWith(Function1<Throwable, UIO<B>> mapError, Function1<T, UIO<B>> map) {
    return new UIO<>(ZIO.redeem(value).foldM(error -> mapError.apply(error).value, x -> map.apply(x).value));
  }

  public UIO<T> repeat() {
    return repeat(1);
  }

  public UIO<T> repeat(int times) {
    return repeat(unit(), times);
  }

  public UIO<T> repeat(Duration delay) {
    return repeat(delay, 1);
  }

  public UIO<T> repeat(Duration delay, int times) {
    return repeat(sleep(delay), times);
  }

  public UIO<T> retry() {
    return retry(1);
  }

  public UIO<T> retry(int maxRetries) {
    return retry(unit(), maxRetries);
  }

  public UIO<T> retry(Duration delay) {
    return retry(delay, 1);
  }

  public UIO<T> retry(Duration delay, int maxRetries) {
    return retry(sleep(delay), maxRetries);
  }

  private UIO<T> repeat(UIO<Unit> pause, int times) {
    return redeemWith(UIO::raiseError, value -> {
      if (times > 0)
        return pause.andThen(repeat(pause, times - 1));
      else
        return pure(value);
    });
  }

  private UIO<T> retry(UIO<Unit> pause, int maxRetries) {
    return redeemWith(error -> {
      if (maxRetries > 0)
        return pause.andThen(retry(pause.repeat(), maxRetries - 1));
      else
        return raiseError(error);
    }, UIO::pure);
  }

  public static <A, B, C> UIO<C> map2(UIO<A> za, UIO<B> zb, Function2<A, B, C> mapper) {
    return new UIO<>(ZIO.map2(za.value, zb.value, mapper));
  }

  public static UIO<Unit> sleep(Duration delay) {
    return exec(() -> Thread.sleep(delay.toMillis()));
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
    return new UIO<>(ZIO.defer(() -> lazy.get().value));
  }

  public static <A> UIO<A> task(Producer<A> task) {
    return fold(ZIO.task(task));
  }

  public static <A extends AutoCloseable, B> UIO<B> bracket(UIO<A> acquire, Function1<A, UIO<B>> use) {
    return fold(ZIO.bracket(ZIO.redeem(acquire.value), resource -> ZIO.redeem(use.apply(resource).value)));
  }

  public static <A, B> UIO<B> bracket(UIO<A> acquire, Function1<A, UIO<B>> use, Consumer1<A> release) {
    return fold(ZIO.bracket(ZIO.redeem(acquire.value), resource -> ZIO.redeem(use.apply(resource).value), release));
  }

  public static UIO<Unit> unit() {
    return new UIO<>(ZIO.unit());
  }

  private static <A> UIO<A> fold(ZIO<Nothing, Throwable, A> zio) {
    return new UIO<>(zio.foldM(error -> UIO.<A>raiseError(error).value, value -> UIO.pure(value).value));
  }
}
