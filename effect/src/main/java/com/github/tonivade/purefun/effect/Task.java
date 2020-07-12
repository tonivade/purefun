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
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.concurrent.Future;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Try;
import com.github.tonivade.purefun.typeclasses.MonadDefer;

@HigherKind
public final class Task<T> implements TaskOf<T>, Recoverable {

  private static final Task<Unit> UNIT = new Task<>(ZIO.unit());

  private final ZIO<Nothing, Throwable, T> instance;

  Task(ZIO<Nothing, Throwable, T> value) {
    this.instance = checkNonNull(value);
  }

  @SuppressWarnings("unchecked")
  public <R> ZIO<R, Throwable, T> toZIO() {
    return (ZIO<R, Throwable, T>) instance;
  }

  public EIO<Throwable, T> toEIO() {
    return new EIO<>(instance);
  }

  @SuppressWarnings("unchecked")
  public <R> RIO<R, T> toURIO() {
    return new RIO<>((ZIO<R, Throwable, T>)instance);
  }

  public Try<T> safeRunSync() {
    return Try.fromEither(instance.provide(nothing()));
  }

  public Future<Try<T>> toFuture() {
    return toFuture(Future.DEFAULT_EXECUTOR);
  }

  public Future<Try<T>> toFuture(Executor executor) {
    return instance.toFuture(executor, nothing()).map(Try::fromEither);
  }

  public void safeRunAsync(Executor executor, Consumer1<Try<T>> callback) {
    instance.provideAsync(executor, nothing(), result -> callback.accept(flatAbsorb(result)));
  }

  public void safeRunAsync(Consumer1<Try<T>> callback) {
    safeRunAsync(Future.DEFAULT_EXECUTOR, callback);
  }

  public <F extends Witness> Kind<F, T> foldMap(MonadDefer<F> monad) {
    return instance.foldMap(nothing(), monad);
  }

  public <B> Task<B> map(Function1<T, B> map) {
    return flatMap(lift(map));
  }

  public <B> Task<B> flatMap(Function1<T, Task<B>> map) {
    return new Task<>(instance.flatMap(value -> map.apply(value).instance));
  }

  public <B> Task<B> andThen(Task<B> next) {
    return new Task<>(instance.andThen(next.instance));
  }

  public <B> Task<B> foldM(Function1<Throwable, Task<B>> mapError, Function1<T, Task<B>> map) {
    return new Task<>(instance.foldM(error -> mapError.apply(error).instance, value -> map.apply(value).instance));
  }

  public <B> UIO<B> fold(Function1<Throwable, B> mapError, Function1<T, B> map) {
    return new UIO<>(instance.fold(mapError, map));
  }

  public UIO<T> recover(Function1<Throwable, T> mapError) {
    return new UIO<>(instance.recover(mapError));
  }

  public Task<T> orElse(Task<T> other) {
    return new Task<>(instance.orElse(other.instance));
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

  public Task<Tuple2<Duration, T>> timed() {
    return new Task<>(instance.timed());
  }
  
  public UIO<T> orDie() {
    return recover(error -> { throw error; });
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
    return new Task<>(ZIO.map2(za.instance, zb.instance, mapper));
  }

  public static <A> Task<A> absorb(Task<Either<Throwable, A>> value) {
    return new Task<>(ZIO.absorb(value.instance));
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
    return new Task<>(ZIO.defer(() -> lazy.get().instance));
  }

  public static <A> Task<A> task(Producer<A> task) {
    return new Task<>(ZIO.task(task));
  }

  public static <A> Task<A> raiseError(Throwable error) {
    return new Task<>(ZIO.raiseError(error));
  }

  public static <A extends AutoCloseable, B> Task<B> bracket(Task<A> acquire, Function1<A, Task<B>> use) {
    return new Task<>(ZIO.bracket(acquire.instance, resource -> use.apply(resource).instance));
  }

  public static <A, B> Task<B> bracket(Task<A> acquire, Function1<A, Task<B>> use, Consumer1<A> release) {
    return new Task<>(ZIO.bracket(acquire.instance, resource -> use.apply(resource).instance, release));
  }

  public static Task<Unit> unit() {
    return UNIT;
  }

  private Try<T> flatAbsorb(Try<Either<Throwable, T>> result) {
    return result.map(Try::fromEither).flatMap(identity());
  }
}
