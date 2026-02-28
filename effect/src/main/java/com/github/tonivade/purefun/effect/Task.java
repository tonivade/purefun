/*
 * Copyright (c) 2018-2026, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.effect;

import static com.github.tonivade.purefun.core.Function2.first;
import static com.github.tonivade.purefun.core.Function2.second;
import static com.github.tonivade.purefun.core.Precondition.checkNonNull;
import static com.github.tonivade.purefun.core.Producer.cons;

import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeoutException;

import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.concurrent.Future;
import com.github.tonivade.purefun.core.CheckedRunnable;
import com.github.tonivade.purefun.core.Consumer1;
import com.github.tonivade.purefun.core.Effect;
import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Function2;
import com.github.tonivade.purefun.core.Producer;
import com.github.tonivade.purefun.core.Recoverable;
import com.github.tonivade.purefun.core.Tuple;
import com.github.tonivade.purefun.core.Tuple2;
import com.github.tonivade.purefun.core.Unit;
import com.github.tonivade.purefun.data.ImmutableList;
import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.type.Try;
import com.github.tonivade.purefun.typeclasses.Fiber;
import com.github.tonivade.purefun.typeclasses.FunctionK;

@HigherKind
public final class Task<A> implements TaskOf<A>, Effect<Task<?>, A>, Recoverable {

  private static final Task<Unit> UNIT = new Task<>(PureIO.unit());

  private final PureIO<Void, Throwable, A> instance;

  Task(PureIO<Void, Throwable, A> value) {
    this.instance = checkNonNull(value);
  }

  @SuppressWarnings("unchecked")
  public <R> PureIO<R, Throwable, A> toPureIO() {
    return (PureIO<R, Throwable, A>) instance;
  }

  public EIO<Throwable, A> toEIO() {
    return new EIO<>(instance);
  }

  @SuppressWarnings("unchecked")
  public <R> RIO<R, A> toURIO() {
    return new RIO<>((PureIO<R, Throwable, A>)instance);
  }

  public UIO<A> toUIO() {
    return recover(this::sneakyThrow);
  }

  public Try<A> safeRunSync() {
    return Try.fromEither(instance.provide(null));
  }

  public Future<A> runAsync() {
    return instance.runAsync(null).flatMap(e -> e.fold(Future::failure, Future::success));
  }

  public Future<A> runAsync(Executor executor) {
    return Task.forked(executor).andThen(this).runAsync();
  }

  public void safeRunAsync(Consumer1<? super Try<? extends A>> callback) {
    instance.provideAsync(null, result -> callback.accept(result.flatMap(Try::fromEither)));
  }

  @Override
  public <B> Task<B> map(Function1<? super A, ? extends B> map) {
    return flatMap(lift(map));
  }

  @Override
  public <B> Task<B> flatMap(Function1<? super A, ? extends Kind<Task<?>, ? extends B>> map) {
    return new Task<>(instance.flatMap(value -> {
      Task<? extends B> apply = map.andThen(TaskOf::toTask).apply(value);
      return apply.instance;
    }));
  }

  @Override
  public <B> Task<B> andThen(Kind<Task<?>, ? extends B> next) {
    return new Task<>(instance.andThen(next.fix(TaskOf::toTask).instance));
  }

  @Override
  public <B> Task<B> ap(Kind<Task<?>, ? extends Function1<? super A, ? extends B>> apply) {
    return new Task<>(instance.ap(apply.fix(TaskOf::toTask).instance));
  }

  public <B> Task<B> foldM(
      Function1<? super Throwable, ? extends Kind<Task<?>, ? extends B>> mapError,
      Function1<? super A, ? extends Kind<Task<?>, ? extends B>> map) {
    return new Task<>(instance.foldM(
        error -> mapError.andThen(TaskOf::toTask).apply(error).instance,
        value -> map.andThen(TaskOf::toTask).apply(value).instance));
  }

  public <B> UIO<B> fold(
      Function1<? super Throwable, ? extends B> mapError, Function1<? super A, ? extends B> map) {
    return new UIO<>(instance.fold(mapError, map).toPureIO());
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

  public UIO<A> recover(Function1<? super Throwable, ? extends A> mapError) {
    return new UIO<>(instance.recover(mapError).toPureIO());
  }

  public Task<A> orElse(Kind<Task<?>, ? extends A> other) {
    return new Task<>(instance.orElse(other.fix(TaskOf::toTask).instance));
  }

  @Override
  public <B> Task<Tuple2<A, B>> zip(Kind<Task<?>, ? extends B> other) {
    return zipWith(other, Tuple::of);
  }

  @Override
  public <B> Task<A> zipLeft(Kind<Task<?>, ? extends B> other) {
    return zipWith(other, first());
  }

  @Override
  public <B> Task<B> zipRight(Kind<Task<?>, ? extends B> other) {
    return zipWith(other, second());
  }

  @Override
  public <B, C> Task<C> zipWith(Kind<Task<?>, ? extends B> other,
      Function2<? super A, ? super B, ? extends C> mapper) {
    return parMap2(this, other.fix(TaskOf::toTask), mapper);
  }

  public Task<Fiber<Task<?>, A>> fork() {
    return new Task<>(instance.fork().map(f -> f.<Task<?>>mapK(new FunctionK<>() {
      @Override
      public <T> Task<T> apply(Kind<PureIO<Void, Throwable, ?>, ? extends T> from) {
        return new Task<>(from.fix(PureIOOf::toPureIO));
      }
    })));
  }

  @Override
  public Task<A> timeout(Duration duration) {
    return timeout(Future.DEFAULT_EXECUTOR, duration);
  }

  public Task<A> timeout(Executor executor, Duration duration) {
    return racePair(executor, this, sleep(duration)).flatMap(either -> either.fold(
        ta -> ta.get2().cancel().fix(TaskOf::toTask).map(x -> ta.get1()),
        tb -> tb.get1().cancel().fix(TaskOf::toTask).flatMap(x -> Task.raiseError(new TimeoutException()))));
  }

  @Override
  public Task<A> repeat() {
    return repeat(1);
  }

  @Override
  public Task<A> repeat(int times) {
    return new Task<>(instance.repeat(times));
  }

  @Override
  public Task<A> repeat(Duration delay) {
    return repeat(delay, 1);
  }

  @Override
  public Task<A> repeat(Duration delay, int times) {
    return new Task<>(instance.repeat(delay, times));
  }

  @Override
  public Task<A> retry() {
    return retry(1);
  }

  @Override
  public Task<A> retry(int maxRetries) {
    return new Task<>(instance.retry(maxRetries));
  }

  @Override
  public Task<A> retry(Duration delay) {
    return retry(delay, 1);
  }

  @Override
  public Task<A> retry(Duration delay, int maxRetries) {
    return new Task<>(instance.retry(delay, maxRetries));
  }

  @Override
  public Task<Tuple2<Duration, A>> timed() {
    return new Task<>(instance.timed());
  }

  public static Task<Unit> forked(Executor executor) {
    return async(callback -> executor.execute(() -> callback.accept(Try.success(Unit.unit()))));
  }

  public static <A, B, C> Task<C> parMap2(Kind<Task<?>, ? extends A> za, Kind<Task<?>, ? extends B> zb,
      Function2<? super A, ? super B, ? extends C> mapper) {
    return parMap2(Future.DEFAULT_EXECUTOR, za, zb, mapper);
  }

  public static <A, B, C> Task<C> parMap2(Executor executor, Kind<Task<?>, ? extends A> za, Kind<Task<?>, ? extends B> zb,
      Function2<? super A, ? super B, ? extends C> mapper) {
    return new Task<>(PureIO.parMap2(executor, za.fix(TaskOf::toTask).instance, zb.fix(TaskOf::toTask).instance, mapper));
  }

  public static <A, B> Task<Either<A, B>> race(Kind<Task<?>, ? extends A> fa, Kind<Task<?>, ? extends B> fb) {
    return race(Future.DEFAULT_EXECUTOR, fa, fb);
  }

  public static <A, B> Task<Either<A, B>> race(Executor executor, Kind<Task<?>, ? extends A> fa, Kind<Task<?>, ? extends B> fb) {
    return racePair(executor, fa, fb).flatMap(either -> either.fold(
        ta -> ta.get2().cancel().fix(TaskOf::toTask).map(x -> Either.left(ta.get1())),
        tb -> tb.get1().cancel().fix(TaskOf::toTask).map(x -> Either.right(tb.get2()))));
  }

  public static <A, B> Task<Either<Tuple2<A, Fiber<Task<?>, B>>, Tuple2<Fiber<Task<?>, A>, B>>>
      racePair(Executor executor, Kind<Task<?>, ? extends A> fa, Kind<Task<?>, ? extends B> fb) {
    PureIO<Void, Throwable, A> instance1 = fa.fix(TaskOf::toTask).instance.fix(PureIOOf::toPureIO);
    PureIO<Void, Throwable, B> instance2 = fb.fix(TaskOf::toTask).instance.fix(PureIOOf::toPureIO);
    return new Task<>(PureIO.racePair(executor, instance1, instance2).map(
      either -> either.bimap(a -> a.map2(f -> f.<Task<?>>mapK(new FunctionK<>() {
        @Override
        public <T> Task<T> apply(Kind<PureIO<Void, Throwable, ?>, ? extends T> from) {
          return new Task<>(from.fix(PureIOOf::toPureIO));
        }
      })), b -> b.map1(f -> f.<Task<?>>mapK(new FunctionK<>() {
        @Override
        public <T> Task<T> apply(Kind<PureIO<Void, Throwable, ?>, ? extends T> from) {
          return new Task<>(from.fix(PureIOOf::toPureIO));
        }
      })))));
  }

  public static <A> Task<A> absorb(Task<Either<Throwable, A>> value) {
    return new Task<>(PureIO.absorb(value.instance));
  }

  public static <A, B> Function1<A, Task<B>> lift(Function1<? super A, ? extends B> function) {
    return PureIO.<Void, A, B>lift(function).andThen(Task::new);
  }

  public static <A, B> Function1<A, Task<B>> liftOption(Function1<? super A, ? extends Option<? extends B>> function) {
    return value -> fromOption(function.apply(value));
  }

  public static <A, B> Function1<A, Task<B>> liftTry(Function1<? super A, ? extends Try<? extends B>> function) {
    return value -> fromTry(function.apply(value));
  }

  public static <A, B> Function1<A, Task<B>> liftEither(Function1<? super A, ? extends Either<Throwable, ? extends B>> function) {
    return value -> fromEither(function.apply(value));
  }

  public static <A> Task<A> fromOption(Option<? extends A> task) {
    return fromOption(cons(task));
  }

  public static <A> Task<A> fromOption(Producer<Option<? extends A>> task) {
    return fromEither(task.andThen(Option::toEither));
  }

  public static <A> Task<A> fromTry(Try<? extends A> task) {
    return fromTry(cons(task));
  }

  public static <A> Task<A> fromTry(Producer<Try<? extends A>> task) {
    return fromEither(task.andThen(Try::toEither));
  }

  public static <A> Task<A> fromEither(Either<Throwable, ? extends A> task) {
    return fromEither(cons(task));
  }

  public static <A> Task<A> fromEither(Producer<Either<Throwable, ? extends A>> task) {
    return new Task<>(PureIO.fromEither(task));
  }

  public static Task<Unit> sleep(Duration delay) {
    return sleep(Future.DEFAULT_EXECUTOR, delay);
  }

  public static Task<Unit> sleep(Executor executor, Duration delay) {
    return new Task<>(PureIO.sleep(executor, delay));
  }

  public static Task<Unit> exec(CheckedRunnable task) {
    return new Task<>(PureIO.exec(task));
  }

  public static <A> Task<A> pure(A value) {
    return new Task<>(PureIO.pure(value));
  }

  public static <A> Task<A> defer(Producer<? extends Kind<Task<?>, ? extends A>> lazy) {
    return new Task<>(PureIO.defer(() -> lazy.andThen(TaskOf::toTask).get().instance));
  }

  public static <A> Task<A> task(Producer<? extends A> task) {
    return new Task<>(PureIO.task(task));
  }

  public static <A> Task<A> never() {
    return async(cb -> {});
  }

  public static <A> Task<A> async(Consumer1<Consumer1<? super Try<? extends A>>> consumer) {
    return new Task<>(PureIO.async(
      (env, cb1) -> consumer.accept(result -> cb1.accept(result.map(Either::right)))));
  }

  public static <A> Task<A> asyncF(Function1<Consumer1<? super Try<? extends A>>, Task<Unit>> consumer) {
    return new Task<>(PureIO.cancellable(
      (env, cb1) -> consumer.andThen(Task::<Void>toPureIO).apply(result -> cb1.accept(result.map(Either::right)))));
  }

  public static <A> Task<A> raiseError(Throwable error) {
    return new Task<>(PureIO.raiseError(error));
  }

  public static <A> Task<Sequence<A>> traverse(Sequence<? extends Kind<Task<?>, A>> sequence) {
    return traverse(Future.DEFAULT_EXECUTOR, sequence);
  }

  public static <A> Task<Sequence<A>> traverse(Executor executor, Sequence<? extends Kind<Task<?>, A>> sequence) {
    return sequence.foldLeft(pure(ImmutableList.empty()),
        (Kind<Task<?>, Sequence<A>> xs, Kind<Task<?>, A> a) -> parMap2(executor, xs, a, Sequence::append));
  }

  public static <A extends AutoCloseable, B> Task<B> bracket(
      Kind<Task<?>, ? extends A> acquire, Function1<? super A, ? extends Kind<Task<?>, ? extends B>> use) {
    return new Task<>(PureIO.bracket(acquire.fix(TaskOf::toTask).instance, resource -> use.andThen(TaskOf::toTask).apply(resource).instance));
  }

  public static <A, B> Task<B> bracket(
      Kind<Task<?>, ? extends A> acquire, Function1<? super A, ? extends Kind<Task<?>, ? extends B>> use, Consumer1<? super A> release) {
    return new Task<>(PureIO.bracket(acquire.fix(TaskOf::toTask).instance, resource -> use.andThen(TaskOf::toTask).apply(resource).instance, release));
  }

  public static <A, B> Task<B> bracket(
      Kind<Task<?>, ? extends A> acquire, Function1<? super A, ? extends Kind<Task<?>, ? extends B>> use, Function1<? super A, ? extends Kind<Task<?>, Unit>> release) {
    return new Task<>(PureIO.bracket(acquire.fix(TaskOf::toTask).instance, resource -> use.andThen(TaskOf::toTask).apply(resource).instance, release.andThen(TaskOf::toTask).andThen(Task::toPureIO)));
  }

  public static Task<Unit> unit() {
    return UNIT;
  }
}
