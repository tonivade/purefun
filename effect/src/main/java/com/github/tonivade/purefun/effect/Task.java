/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.effect;

import static com.github.tonivade.purefun.Function1.identity;
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
import com.github.tonivade.purefun.Recoverable;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.concurrent.Future;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Try;
import com.github.tonivade.purefun.typeclasses.MonadDefer;

@HigherKind
public final class Task<T> implements TaskOf<T>, Recoverable {

  private final ZIO<Nothing, Throwable, T> value;

  Task(ZIO<Nothing, Throwable, T> value) {
    this.value = checkNonNull(value);
  }

  @SuppressWarnings("unchecked")
  public <R> ZIO<R, Throwable, T> toZIO() {
    return (ZIO<R, Throwable, T>) value;
  }

  public EIO<Throwable, T> toEIO() {
    return new EIO<>(value);
  }

  public Try<T> safeRunSync() {
    return absorb(value.provide(nothing()));
  }

  public Future<Try<T>> toFuture() {
    return value.toFuture(nothing()).map(this::absorb);
  }

  public void async(Executor executor, Consumer1<Try<T>> callback) {
    value.provideAsync(executor, nothing(), result -> callback.accept(flatAbsorb(result)));
  }

  public void async(Consumer1<Try<T>> callback) {
    async(Future.DEFAULT_EXECUTOR, callback);
  }

  public <F extends Witness> Kind<F, T> foldMap(MonadDefer<F> monad) {
    return monad.flatMap(value.foldMap(nothing(), monad), monad::<T>fromEither);
  }

  public <B> Task<B> map(Function1<T, B> map) {
    return flatMap(lift(map));
  }

  public <B> Task<B> flatMap(Function1<T, Task<B>> map) {
    return new Task<>(value.flatMap(value -> map.apply(value).value));
  }

  public <B> Task<B> andThen(Task<B> next) {
    return new Task<>(value.andThen(next.value));
  }

  public <B> Task<B> foldM(Function1<Throwable, Task<B>> mapError, Function1<T, Task<B>> map) {
    return new Task<>(value.foldM(error -> mapError.apply(error).value, value -> map.apply(value).value));
  }

  public <B> UIO<B> fold(Function1<Throwable, B> mapError, Function1<T, B> map) {
    return new UIO<>(value.fold(mapError, map));
  }

  public UIO<T> recover(Function1<Throwable, T> mapError) {
    return new UIO<>(value.recover(mapError));
  }

  public Task<T> orElse(Producer<Task<T>> other) {
    return new Task<>(value.orElse(() -> other.get().value));
  }

  public Task<T> repeat() {
    return repeat(1);
  }

  public Task<T> repeat(int times) {
    return repeat(unit(), times);
  }

  public Task<T> repeat(Duration delay) {
    return repeat(delay, 1);
  }

  public Task<T> repeat(Duration delay, int times) {
    return repeat(sleep(delay), times);
  }

  public Task<T> retry() {
    return retry(1);
  }

  public Task<T> retry(int maxRetries) {
    return retry(unit(), maxRetries);
  }

  public Task<T> retry(Duration delay) {
    return retry(delay, 1);
  }

  public Task<T> retry(Duration delay, int maxRetries) {
    return retry(sleep(delay), maxRetries);
  }

  private Task<T> repeat(Task<Unit> pause, int times) {
    return foldM(Task::raiseError, value -> {
      if (times > 0) {
        return pause.andThen(repeat(pause, times - 1));
      } else {
        return pure(value);
      }
    });
  }

  private Task<T> retry(Task<Unit> pause, int maxRetries) {
    return foldM(error -> {
      if (maxRetries > 0) {
        return pause.andThen(retry(pause.repeat(), maxRetries - 1));
      } else {
        return raiseError(error);
      }
    }, Task::pure);
  }

  public static <A, B, C> Task<C> map2(Task<A> za, Task<B> zb, Function2<A, B, C> mapper) {
    return new Task<>(ZIO.map2(za.value, zb.value, mapper));
  }

  public static <A> Task<A> absorb(Task<Either<Throwable, A>> value) {
    return new Task<>(ZIO.absorb(value.value));
  }

  public static <A, B> Function1<A, Task<B>> lift(Function1<A, B> function) {
    return ZIO.<Nothing, A, B>lift(function).andThen(Task::new);
  }

  public static <A> Task<A> fromEither(Producer<Either<Throwable, A>> task) {
    return new Task<>(ZIO.fromEither(task));
  }

  public static Task<Unit> sleep(Duration delay) {
    return new Task<>(ZIO.sleep(delay));
  }

  public static Task<Unit> exec(CheckedRunnable task) {
    return new Task<>(ZIO.exec(task));
  }

  public static <A> Task<A> pure(A value) {
    return new Task<>(ZIO.pure(value));
  }

  public static <A> Task<A> defer(Producer<Task<A>> lazy) {
    return new Task<>(ZIO.defer(() -> lazy.get().value));
  }

  public static <A> Task<A> task(Producer<A> task) {
    return new Task<>(ZIO.task(task));
  }

  public static <A> Task<A> raiseError(Throwable error) {
    return new Task<>(ZIO.raiseError(error));
  }

  public static <A extends AutoCloseable, B> Task<B> bracket(Task<A> acquire, Function1<A, Task<B>> use) {
    return new Task<>(ZIO.bracket(acquire.value, resource -> use.apply(resource).value));
  }

  public static <A, B> Task<B> bracket(Task<A> acquire, Function1<A, Task<B>> use, Consumer1<A> release) {
    return new Task<>(ZIO.bracket(acquire.value, resource -> use.apply(resource).value, release));
  }

  public static Task<Unit> unit() {
    return new Task<>(ZIO.unit());
  }

  private Try<T> flatAbsorb(Try<Either<Throwable, T>> result) {
    return result.map(this::absorb).flatMap(identity());
  }

  private Try<T> absorb(Either<Throwable, T> either) {
    return either.fold(Try::failure, Try::success);
  }
}
