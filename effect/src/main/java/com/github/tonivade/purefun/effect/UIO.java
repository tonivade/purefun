/*
 * Copyright (c) 2018-2026, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.effect;

import static com.github.tonivade.purefun.core.Function1.identity;
import static com.github.tonivade.purefun.core.Function2.first;
import static com.github.tonivade.purefun.core.Function2.second;
import static com.github.tonivade.purefun.core.Precondition.checkNonNull;

import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeoutException;

import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.concurrent.Future;
import com.github.tonivade.purefun.concurrent.Promise;
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
import com.github.tonivade.purefun.data.ImmutableMap;
import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.type.Try;
import com.github.tonivade.purefun.typeclasses.Fiber;
import com.github.tonivade.purefun.typeclasses.FunctionK;

@HigherKind
public final class UIO<A> implements UIOOf<A>, Effect<UIO<?>, A>, Recoverable {

  private static final UIO<Unit> UNIT = new UIO<>(PureIO.unit());

  private final PureIO<Void, Void, A> instance;

  UIO(PureIO<Void, Void, A> value) {
    this.instance = checkNonNull(value);
  }

  public Future<A> runAsync() {
    return instance.runAsync(null).map(Either::getRight);
  }

  public Future<A> runAsync(Executor executor) {
    return UIO.forked(executor).andThen(this).runAsync();
  }

  public A unsafeRunSync() {
    return instance.provide(null).get();
  }

  public Try<A> safeRunSync() {
    return Try.of(this::unsafeRunSync);
  }

  @SuppressWarnings("unchecked")
  public <R, E> PureIO<R, E, A> toPureIO() {
    return (PureIO<R, E, A>) instance;
  }

  @SuppressWarnings("unchecked")
  public <E> EIO<E, A> toEIO() {
    return new EIO<>((PureIO<Void, E, A>) instance);
  }

  @SuppressWarnings("unchecked")
  public <R> RIO<R, A> toRIO() {
    return new RIO<>((PureIO<R, Throwable, A>) PureIO.redeem(instance));
  }

  @SuppressWarnings("unchecked")
  public <R> URIO<R, A> toURIO() {
    return new URIO<>((PureIO<R, Void, A>) instance);
  }

  public Task<A> toTask() {
    return new Task<>(PureIO.redeem(instance));
  }

  public void safeRunAsync(Consumer1<? super Try<? extends A>> callback) {
    safeRunAsync(Future.DEFAULT_EXECUTOR, callback);
  }

  public void safeRunAsync(Executor executor, Consumer1<? super Try<? extends A>> callback) {
    instance.provideAsync(null, executor, x -> callback.accept(x.map(Either::getRight)));
  }

  @Override
  public <B> UIO<B> map(Function1<? super A, ? extends B> map) {
    return new UIO<>(instance.map(map));
  }

  @Override
  public <B> UIO<B> flatMap(Function1<? super A, ? extends Kind<UIO<?>, ? extends B>> map) {
    return new UIO<>(instance.flatMap(x -> {
      UIO<? extends B> apply = map.andThen(UIOOf::toUIO).apply(x);
      return apply.instance;
    }));
  }

  @Override
  public <B> UIO<B> andThen(Kind<UIO<?>, ? extends B> next) {
    return new UIO<>(instance.andThen(next.fix(UIOOf::toUIO).instance));
  }

  @Override
  public <B> UIO<B> ap(Kind<UIO<?>, ? extends Function1<? super A, ? extends B>> apply) {
    return new UIO<>(instance.ap(apply.fix(UIOOf::toUIO).instance));
  }

  public UIO<A> recover(Function1<? super Throwable, ? extends A> mapError) {
    return redeem(mapError, identity());
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

  public <B> UIO<B> redeem(
      Function1<? super Throwable, ? extends B> mapError, Function1<? super A, ? extends B> map) {
    return redeemWith(mapError.andThen(UIO::pure), map.andThen(UIO::pure));
  }

  public <B> UIO<B> redeemWith(
      Function1<? super Throwable, ? extends Kind<UIO<?>, ? extends B>> mapError,
      Function1<? super A, ? extends Kind<UIO<?>, ? extends B>> map) {
    return new UIO<>(PureIO.redeem(instance).foldM(
        error -> mapError.andThen(UIOOf::toUIO).apply(error).instance,
        value -> map.andThen(UIOOf::toUIO).apply(value).instance));
  }

  @Override
  public <B> UIO<Tuple2<A, B>> zip(Kind<UIO<?>, ? extends B> other) {
    return zipWith(other, Tuple::of);
  }

  @Override
  public <B> UIO<A> zipLeft(Kind<UIO<?>, ? extends B> other) {
    return zipWith(other, first());
  }

  @Override
  public <B> UIO<B> zipRight(Kind<UIO<?>, ? extends B> other) {
    return zipWith(other, second());
  }

  @Override
  public <B, C> UIO<C> zipWith(Kind<UIO<?>, ? extends B> other,
      Function2<? super A, ? super B, ? extends C> mapper) {
    return parMap2(this, other.fix(UIOOf::toUIO), mapper);
  }

  public UIO<Fiber<UIO<?>, A>> fork() {
    return new UIO<>(instance.fork().map(f -> f.<UIO<?>>mapK(new FunctionK<>() {
      @Override
      public <T> UIO<T> apply(Kind<PureIO<Void, Void, ?>, ? extends T> from) {
        return new UIO<>(from.fix(PureIOOf::toPureIO));
      }
    })));
  }

  @Override
  public UIO<A> timeout(Duration duration) {
    return timeout(Future.DEFAULT_EXECUTOR, duration);
  }

  public UIO<A> timeout(Executor executor, Duration duration) {
    return racePair(executor, this, sleep(duration)).flatMap(either -> either.fold(
        ta -> ta.get2().cancel().fix(UIOOf::toUIO).map(x -> ta.get1()),
        tb -> tb.get1().cancel().fix(UIOOf::toUIO).flatMap(x -> UIO.raiseError(new TimeoutException()))));
  }

  @Override
  public UIO<A> repeat() {
    return repeat(1);
  }

  @Override
  public UIO<A> repeat(int times) {
    return repeat(this, unit(), times);
  }

  @Override
  public UIO<A> repeat(Duration delay) {
    return repeat(delay, 1);
  }

  @Override
  public UIO<A> repeat(Duration delay, int times) {
    return repeat(this, sleep(delay), times);
  }

  @Override
  public UIO<A> retry() {
    return retry(1);
  }

  @Override
  public UIO<A> retry(int maxRetries) {
    return retry(this, unit(), maxRetries);
  }

  @Override
  public UIO<A> retry(Duration delay) {
    return retry(delay, 1);
  }

  @Override
  public UIO<A> retry(Duration delay, int maxRetries) {
    return retry(this, sleep(delay), maxRetries);
  }

  @Override
  public UIO<Tuple2<Duration, A>> timed() {
    return new UIO<>(instance.timed());
  }

  public static UIO<Unit> forked(Executor executor) {
    return async(callback -> executor.execute(() -> callback.accept(Try.success(Unit.unit()))));
  }

  public static <A, B, C> UIO<C> parMap2(Kind<UIO<?>, ? extends A> za, Kind<UIO<?>, ? extends B> zb,
      Function2<? super A, ? super B, ? extends C> mapper) {
    return parMap2(Future.DEFAULT_EXECUTOR, za, zb, mapper);
  }

  public static <A, B, C> UIO<C> parMap2(Executor executor, Kind<UIO<?>, ? extends A> za, Kind<UIO<?>, ? extends B> zb,
      Function2<? super A, ? super B, ? extends C> mapper) {
    return new UIO<>(PureIO.parMap2(executor, za.fix(UIOOf::toUIO).instance, zb.fix(UIOOf::toUIO).instance, mapper));
  }

  public static <A, B> UIO<Either<A, B>> race(Kind<UIO<?>, ? extends A> fa, Kind<UIO<?>, ? extends B> fb) {
    return race(Future.DEFAULT_EXECUTOR, fa, fb);
  }

  public static <A, B> UIO<Either<A, B>> race(Executor executor, Kind<UIO<?>, ? extends A> fa, Kind<UIO<?>, ? extends B> fb) {
    return racePair(executor, fa, fb).flatMap(either -> either.fold(
        ta -> ta.get2().cancel().fix(UIOOf::toUIO).map(x -> Either.left(ta.get1())),
        tb -> tb.get1().cancel().fix(UIOOf::toUIO).map(x -> Either.right(tb.get2()))));
  }

  public static <A, B> UIO<Either<Tuple2<A, Fiber<UIO<?>, B>>, Tuple2<Fiber<UIO<?>, A>, B>>>
      racePair(Executor executor, Kind<UIO<?>, ? extends A> fa, Kind<UIO<?>, ? extends B> fb) {
    PureIO<Void, Void, A> instance1 = fa.fix(UIOOf::toUIO).instance.fix(PureIOOf::toPureIO);
    PureIO<Void, Void, B> instance2 = fb.fix(UIOOf::toUIO).instance.fix(PureIOOf::toPureIO);
    return new UIO<>(PureIO.racePair(executor, instance1, instance2).map(
      either -> either.bimap(a -> a.map2(f -> f.<UIO<?>>mapK(new FunctionK<>() {
        @Override
        public <T> UIO<T> apply(Kind<PureIO<Void, Void, ?>, ? extends T> from) {
          return new UIO<>(from.fix(PureIOOf::toPureIO));
        }
      })), b -> b.map1(f -> f.<UIO<?>>mapK(new FunctionK<>() {
        @Override
        public <T> UIO<T> apply(Kind<PureIO<Void, Void, ?>, ? extends T> from) {
          return new UIO<>(from.fix(PureIOOf::toPureIO));
        }
      })))));
  }

  public static <A, B> Function1<A, UIO<B>> lift(Function1<? super A, ? extends B> function) {
    return value -> task(() -> function.apply(value));
  }

  public static <A, B> Function1<A, UIO<B>> liftOption(Function1<? super A, ? extends Option<? extends B>> function) {
    return value -> fromOption(function.apply(value));
  }

  public static <A, B> Function1<A, UIO<B>> liftTry(Function1<? super A, ? extends Try<? extends B>> function) {
    return value -> fromTry(function.apply(value));
  }

  public static <A, B> Function1<A, UIO<B>> liftEither(Function1<? super A, ? extends Either<Throwable, ? extends B>> function) {
    return value -> fromEither(function.apply(value));
  }

  public static UIO<Unit> sleep(Duration delay) {
    return sleep(Future.DEFAULT_EXECUTOR, delay);
  }

  public static UIO<Unit> sleep(Executor executor, Duration delay) {
    return fold(PureIO.sleep(executor, delay));
  }

  public static UIO<Unit> exec(CheckedRunnable task) {
    return fold(PureIO.exec(task));
  }

  public static <A> UIO<A> pure(A value) {
    return new UIO<>(PureIO.pure(value));
  }

  public static <A> UIO<A> raiseError(Throwable throwable) {
    return new UIO<>(PureIO.fromEither(() -> { throw throwable; }));
  }

  public static <A> UIO<A> defer(Producer<Kind<UIO<?>, ? extends A>> lazy) {
    return new UIO<>(PureIO.defer(() -> lazy.andThen(UIOOf::toUIO).get().instance));
  }

  public static <A> UIO<A> task(Producer<? extends A> task) {
    return fold(PureIO.task(task));
  }

  public static <T> UIO<T> fromOption(Option<? extends T> task) {
    return fromEither(task.toEither());
  }

  public static <T> UIO<T> fromTry(Try<? extends T> task) {
    return fromEither(task.toEither());
  }

  public static <T> UIO<T> fromEither(Either<Throwable, ? extends T> task) {
    return task.fold(UIO::raiseError, UIO::pure);
  }

  public static <T> UIO<T> fromPromise(Promise<? extends T> promise) {
    Consumer1<Consumer1<? super Try<? extends T>>> consumer = promise::onComplete;
    return async(consumer);
  }

  public static <A> UIO<A> never() {
    return async(cb -> {});
  }

  public static <A> UIO<A> async(Consumer1<Consumer1<? super Try<? extends A>>> consumer) {
    return fold(PureIO.async(
        (env, cb1) -> consumer.accept(result -> cb1.accept(result.map(Either::right)))));
  }

  public static <A> UIO<A> cancellable(Function1<Consumer1<? super Try<? extends A>>, UIO<Unit>> consumer) {
    return fold(PureIO.cancellable(
        (env, cb1) -> consumer.andThen(UIO::<Void, Throwable>toPureIO).apply(result -> cb1.accept(result.map(Either::right)))));
  }

  public static <A, T> UIO<Function1<A, UIO<T>>> memoize(Function1<A, UIO<T>> function) {
    return memoize(Future.DEFAULT_EXECUTOR, function);
  }

  public static <A, T> UIO<Function1<A, UIO<T>>> memoize(Executor executor, Function1<A, UIO<T>> function) {
    var ref = Ref.make(ImmutableMap.<A, Promise<T>>empty());
    return ref.map(r -> {
      Function1<A, UIO<UIO<T>>> result = a -> r.modify(map -> map.get(a).fold(() -> {
        Promise<T> promise = Promise.make();
        function.apply(a).safeRunAsync(executor, promise::tryComplete);
        return Tuple.of(UIO.fromPromise(promise), map.put(a, promise));
      }, promise -> Tuple.of(UIO.fromPromise(promise), map)));
      return result.andThen(io -> io.flatMap(identity()));
    });
  }

  public static <A> UIO<Sequence<A>> traverse(Sequence<? extends Kind<UIO<?>, A>> sequence) {
    return traverse(Future.DEFAULT_EXECUTOR, sequence);
  }

  public static <A> UIO<Sequence<A>> traverse(Executor executor, Sequence<? extends Kind<UIO<?>, A>> sequence) {
    return sequence.foldLeft(pure(ImmutableList.empty()),
        (Kind<UIO<?>, Sequence<A>> xs, Kind<UIO<?>, A> a) -> parMap2(executor, xs, a, Sequence::append));
  }

  public static <A extends AutoCloseable, B> UIO<B> bracket(
      Kind<UIO<?>, ? extends A> acquire, Function1<? super A, ? extends Kind<UIO<?>, ? extends B>> use) {
    return fold(PureIO.bracket(PureIO.redeem(acquire.fix(UIOOf::toUIO).instance),
        resource -> PureIO.redeem(use.andThen(UIOOf::toUIO).apply(resource).instance)));
  }

  public static <A, B> UIO<B> bracket(Kind<UIO<?>, ? extends A> acquire,
      Function1<? super A, ? extends Kind<UIO<?>, ? extends B>> use, Consumer1<? super A> release) {
    return fold(PureIO.bracket(PureIO.redeem(acquire.fix(UIOOf::toUIO).instance),
        resource -> PureIO.redeem(use.andThen(UIOOf::toUIO).apply(resource).instance), release));
  }

  public static <A, B> UIO<B> bracket(Kind<UIO<?>, ? extends A> acquire,
      Function1<? super A, ? extends Kind<UIO<?>, ? extends B>> use, Function1<? super A, ? extends Kind<UIO<?>, Unit>> release) {
    return fold(PureIO.bracket(PureIO.redeem(acquire.fix(UIOOf::toUIO).instance),
        resource -> PureIO.redeem(use.andThen(UIOOf::toUIO).apply(resource).instance), release.andThen(UIOOf::toUIO).andThen(UIO::toPureIO)));
  }

  public static UIO<Unit> unit() {
    return UNIT;
  }

  private static <A> UIO<A> fold(PureIO<Void, Throwable, A> zio) {
    return new UIO<>(zio.foldM(error -> UIO.<A>raiseError(error).instance, value -> UIO.pure(value).instance));
  }

  private static <T> UIO<T> repeat(UIO<T> self, UIO<Unit> pause, int times) {
    return self.redeemWith(UIO::raiseError, value -> {
      if (times > 0) {
        return pause.andThen(repeat(self, pause, times - 1));
      }
      return UIO.pure(value);
    });
  }

  private static <T> UIO<T> retry(UIO<T> self, UIO<Unit> pause, int maxRetries) {
    return self.redeemWith(error -> {
      if (maxRetries > 0) {
        return pause.andThen(retry(self, pause.repeat(), maxRetries - 1));
      }
      return UIO.raiseError(error);
    }, UIO::pure);
  }
}
