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
public final class Task<A> implements TaskOf<A>, Recoverable {

  private static final Task<Unit> UNIT = new Task<>(ZIO.unit());

  private final ZIO<Nothing, Throwable, A> instance;

  Task(ZIO<Nothing, Throwable, A> value) {
    this.instance = checkNonNull(value);
  }

  @SuppressWarnings("unchecked")
  public <R> ZIO<R, Throwable, A> toZIO() {
    return (ZIO<R, Throwable, A>) instance;
  }

  public EIO<Throwable, A> toEIO() {
    return new EIO<>(instance);
  }

  @SuppressWarnings("unchecked")
  public <R> RIO<R, A> toURIO() {
    return new RIO<>((ZIO<R, Throwable, A>)instance);
  }
  
  public UIO<A> toUIO() {
    return recover(this::sneakyThrow);
  }

  public Try<A> safeRunSync() {
    return Try.fromEither(instance.provide(nothing()));
  }

  public Future<Try<A>> toFuture() {
    return toFuture(Future.DEFAULT_EXECUTOR);
  }

  public Future<Try<A>> toFuture(Executor executor) {
    return instance.toFuture(executor, nothing()).map(Try::fromEither);
  }

  public void safeRunAsync(Executor executor, Consumer1<? super Try<? extends A>> callback) {
    instance.provideAsync(executor, nothing(), result -> callback.accept(flatAbsorb(result)));
  }

  public void safeRunAsync(Consumer1<? super Try<? extends A>> callback) {
    safeRunAsync(Future.DEFAULT_EXECUTOR, callback);
  }

  public <F extends Witness> Kind<F, A> foldMap(Async<F> monad) {
    return instance.foldMap(nothing(), monad);
  }

  public <B> Task<B> map(Function1<? super A, ? extends B> map) {
    return flatMap(lift(map));
  }

  public <B> Task<B> flatMap(Function1<? super A, ? extends Task<? extends B>> map) {
    return new Task<>(instance.flatMap(value -> {
      Task<? extends B> apply = map.apply(value);
      return apply.instance;
    }));
  }

  public <B> Task<B> andThen(Task<B> next) {
    return new Task<>(instance.andThen(next.instance));
  }

  public <B> Task<B> ap(Task<Function1<? super A, ? extends B>> apply) {
    return new Task<>(instance.ap(apply.toZIO()));
  }

  public <B> Task<B> foldM(
      Function1<? super Throwable, ? extends Task<? extends B>> mapError, 
      Function1<? super A, ? extends Task<? extends B>> map) {
    return new Task<>(instance.foldM(
        error -> mapError.andThen(TaskOf::narrowK).apply(error).instance, 
        value -> map.andThen(TaskOf::narrowK).apply(value).instance));
  }

  public <B> UIO<B> fold(Function1<Throwable, B> mapError, Function1<A, B> map) {
    return new UIO<>(instance.fold(mapError, map));
  }

  @SuppressWarnings("unchecked")
  public <X extends Throwable> UIO<A> recoverWith(Class<X> type, Function1<X, A> function) {
    return recover(cause -> {
      if (type.isAssignableFrom(cause.getClass())) {
        return function.apply((X) cause);
      }
      return sneakyThrow(cause);
    });
  }

  public UIO<A> recover(Function1<Throwable, A> mapError) {
    return new UIO<>(instance.recover(mapError));
  }

  public Task<A> orElse(Task<A> other) {
    return new Task<>(instance.orElse(other.instance));
  }
  
  public <B> Task<Tuple2<A, B>> zip(Task<B> other) {
    return zipWith(other, Tuple::of);
  }
  
  public <B> Task<A> zipLeft(Task<B> other) {
    return zipWith(other, first());
  }
  
  public <B> Task<B> zipRight(Task<B> other) {
    return zipWith(other, second());
  }
  
  public <B, C> Task<C> zipWith(Task<B> other, Function2<A, B, C> mapper) {
    return map2(this, other, mapper);
  }

  public Task<A> repeat() {
    return repeat(1);
  }

  public Task<A> repeat(int times) {
    return new Task<>(instance.repeat(times));
  }

  public Task<A> repeat(Duration delay) {
    return repeat(delay, 1);
  }

  public Task<A> repeat(Duration delay, int times) {
    return new Task<>(instance.repeat(delay, times));
  }
  
  public <B> Task<B> repeat(Schedule<Nothing, A, B> schedule) {
    return new Task<>(instance.repeat(schedule));
  }

  public Task<A> retry() {
    return retry(1);
  }

  public Task<A> retry(int maxRetries) {
    return retry(Schedule.recurs(maxRetries));
  }

  public Task<A> retry(Duration delay) {
    return retry(delay, 1);
  }

  public Task<A> retry(Duration delay, int maxRetries) {
    return retry(Schedule.<Nothing, Throwable>recursSpaced(delay, maxRetries));
  }
  
  public <B> Task<A> retry(Schedule<Nothing, Throwable, B> schedule) {
    return new Task<>(instance.retry(schedule));
  }

  public Task<Tuple2<Duration, A>> timed() {
    return new Task<>(instance.timed());
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
  
  public static <A> Task<A> async(Consumer1<Consumer1<Try<A>>> consumer) {
    return new Task<>(ZIO.async(consumer));
  }

  public static <A> Task<A> raiseError(Throwable error) {
    return new Task<>(ZIO.raiseError(error));
  }

  public static <A extends AutoCloseable, B> Task<B> bracket(Task<? extends A> acquire, Function1<? super A, ? extends Task<? extends B>> use) {
    return new Task<>(ZIO.bracket(acquire.instance, resource -> use.andThen(TaskOf::narrowK).apply(resource).instance));
  }

  public static <A, B> Task<B> bracket(Task<? extends A> acquire, Function1<? super A, ? extends Task<? extends B>> use, Consumer1<? super A> release) {
    return new Task<>(ZIO.bracket(acquire.instance, resource -> use.andThen(TaskOf::narrowK).apply(resource).instance, release));
  }

  public static Task<Unit> unit() {
    return UNIT;
  }

  private Try<A> flatAbsorb(Try<? extends Either<Throwable, A>> result) {
    return result.map(Try::fromEither).flatMap(identity());
  }
}
