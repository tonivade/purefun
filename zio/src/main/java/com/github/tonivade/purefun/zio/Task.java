/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.zio;

import com.github.tonivade.purefun.CheckedRunnable;
import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Function2;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Nothing;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.concurrent.Future;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Try;
import com.github.tonivade.purefun.typeclasses.MonadDefer;

import java.util.concurrent.Executor;

import static com.github.tonivade.purefun.Function1.identity;
import static com.github.tonivade.purefun.Nothing.nothing;
import static java.util.Objects.requireNonNull;

@HigherKind
public final class Task<T> {

  private final ZIO<Nothing, Throwable, T> value;

  Task(ZIO<Nothing, Throwable, T> value) {
    this.value = requireNonNull(value);
  }

  public <R> ZIO<R, Throwable, T> toZIO() {
    return (ZIO<R, Throwable, T>) value;
  }

  public EIO<Throwable, T> toEIO() {
    return new EIO<>(value);
  }

  public Try<T> safeRunSync() {
    return absorb(value.provide(nothing()));
  }

  public Future<Try<T>> toFuture(Executor executor) {
    return value.toFuture(executor, nothing()).map(this::absorb);
  }

  public Future<Try<T>> toFuture() {
    return toFuture(Future.DEFAULT_EXECUTOR);
  }

  public void async(Executor executor, Consumer1<Try<T>> callback) {
    value.provideAsync(executor, nothing(), result -> callback.accept(flatAbsorb(result)));
  }

  public void async(Consumer1<Try<T>> callback) {
    async(Future.DEFAULT_EXECUTOR, callback);
  }

  public <F extends Kind> Higher1<F, Try<T>> foldMap(MonadDefer<F> monad) {
    return monad.map(value.foldMap(nothing(), monad), this::absorb);
  }

  public <B> Task<B> map(Function1<T, B> map) {
    return new Task<>(value.map(map));
  }

  public <B> Task<B> flatMap(Function1<T, Task<B>> map) {
    return new Task<>(value.flatMap(value -> map.apply(value).value));
  }

  public <B> Task<B> flatten() {
    return new Task<>(value.flatten());
  }

  public <B> Task<B> andThen(Task<B> next) {
    return new Task<>(value.andThen(next.value));
  }

  public <B> Task<B> foldM(Function1<Throwable, Task<B>> mapError, Function1<T, Task<B>> map) {
    return new Task<>(value.foldM(error -> mapError.apply(error).value, value -> map.apply(value).value));
  }

  public <B> UIO<B> flatAbsorb(Function1<Throwable, B> mapError, Function1<T, B> map) {
    return new UIO<>(value.fold(mapError, map));
  }

  public UIO<T> recover(Function1<Throwable, T> mapError) {
    return new UIO<>(value.recover(mapError));
  }

  public Task<T> orElse(Producer<Task<T>> other) {
    return new Task<>(value.orElse(() -> other.get().value));
  }

  public static <A, B, C> Task<C> map2(Task<A> za, Task<B> zb, Function2<A, B, C> mapper) {
    return new Task<>(ZIO.map2(za.value, zb.value, mapper));
  }

  public static <A> Task<A> flatAbsorb(Task<Either<Throwable, A>> value) {
    return new Task<>(ZIO.absorb(value.value));
  }

  public static <A, B> Function1<A, Task<B>> lift(Function1<A, B> function) {
    return ZIO.<Nothing, A, B>lift(function).andThen(Task::new);
  }

  public static <A> Task<A> fromEither(Producer<Either<Throwable, A>> task) {
    return new Task<>(ZIO.fromEither(task));
  }

  public static <A> Task<A> from(Producer<A> task) {
    return new Task<>(ZIO.from(task));
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
