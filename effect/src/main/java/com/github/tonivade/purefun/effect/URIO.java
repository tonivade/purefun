/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
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
import com.github.tonivade.purefun.core.CheckedRunnable;
import com.github.tonivade.purefun.core.Consumer1;
import com.github.tonivade.purefun.core.Consumer2;
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
public final class URIO<R, A> implements URIOOf<R, A>, Effect<URIO<R, ?>, A>, Recoverable {

  private static final URIO<?, Unit> UNIT = new URIO<>(PureIO.unit());

  private final PureIO<R, Void, A> instance;

  URIO(PureIO<R, Void, A> value) {
    this.instance = checkNonNull(value);
  }

  public A unsafeRunSync(R env) {
    return instance.provide(env).get();
  }

  public Try<A> safeRunSync(R env) {
    return Try.of(() -> unsafeRunSync(env));
  }

  @SuppressWarnings("unchecked")
  public <E> PureIO<R, E, A> toPureIO() {
    return (PureIO<R, E, A>) instance;
  }

  @SuppressWarnings("unchecked")
  public <E> EIO<E, A> toEIO() {
    return new EIO<>((PureIO<Void, E, A>) instance);
  }

  public RIO<R, A> toRIO() {
    return new RIO<>(PureIO.redeem(instance));
  }

  public Future<A> runAsync(R env) {
    return instance.runAsync(env).map(Either::getRight);
  }

  public Future<A> runAsync(R env, Executor executor) {
    return URIO.<R>forked(executor).andThen(this).runAsync(env);
  }

  public void safeRunAsync(R env, Consumer1<? super Try<? extends A>> callback) {
    instance.provideAsync(env, result -> callback.accept(result.map(Either::getRight)));
  }

  @Override
  public <B> URIO<R, B> map(Function1<? super A, ? extends B> map) {
    return new URIO<>(instance.map(map));
  }

  @Override
  public <B> URIO<R, B> flatMap(Function1<? super A, ? extends Kind<URIO<R, ?>, ? extends B>> map) {
    return new URIO<>(instance.flatMap(x -> {
      URIO<R, ? extends B> apply = map.andThen(URIOOf::toURIO).apply(x);
      return apply.instance;
    }));
  }

  @Override
  public <B> URIO<R, B> andThen(Kind<URIO<R, ?>, ? extends B> next) {
    return new URIO<>(instance.andThen(next.fix(URIOOf::toURIO).instance));
  }

  @Override
  public <B> URIO<R, B> ap(Kind<URIO<R, ?>, ? extends Function1<? super A, ? extends B>> apply) {
    return new URIO<>(instance.ap(apply.fix(URIOOf::toURIO).instance));
  }

  public URIO<R, A> recover(Function1<? super Throwable, ? extends A> mapError) {
    return redeem(mapError, identity());
  }

  @SuppressWarnings("unchecked")
  public <X extends Throwable> URIO<R, A> recoverWith(Class<X> type,
      Function1<? super X, ? extends A> function) {
    return recover(cause -> {
      if (type.isAssignableFrom(cause.getClass())) {
        return function.apply((X) cause);
      }
      return sneakyThrow(cause);
    });
  }

  public <B> URIO<R, B> redeem(
      Function1<? super Throwable, ? extends B> mapError, Function1<? super A, ? extends B> map) {
    return redeemWith(mapError.andThen(URIO::pure), map.andThen(URIO::pure));
  }

  public <B> URIO<R, B> redeemWith(
      Function1<? super Throwable, ? extends Kind<URIO<R, ?>, ? extends B>> mapError,
      Function1<? super A, ? extends Kind<URIO<R, ?>, ? extends B>> map) {
    return new URIO<>(PureIO.redeem(instance).foldM(
        error -> mapError.andThen(URIOOf::toURIO).apply(error).instance,
        value -> map.andThen(URIOOf::toURIO).apply(value).instance));
  }

  @Override
  public <B> URIO<R, Tuple2<A, B>> zip(Kind<URIO<R, ?>, ? extends B> other) {
    return zipWith(other, Tuple::of);
  }

  @Override
  public <B> URIO<R, A> zipLeft(Kind<URIO<R, ?>, ? extends B> other) {
    return zipWith(other, first());
  }

  @Override
  public <B> URIO<R, B> zipRight(Kind<URIO<R, ?>, ? extends B> other) {
    return zipWith(other, second());
  }

  @Override
  public <B, C> URIO<R, C> zipWith(Kind<URIO<R, ?>, ? extends B> other,
      Function2<? super A, ? super B, ? extends C> mapper) {
    return parMap2(this, other.fix(URIOOf::toURIO), mapper);
  }

  public URIO<R, Fiber<URIO<R, ?>, A>> fork() {
    return new URIO<>(instance.fork().map(f -> f.<URIO<R, ?>>mapK(new FunctionK<>() {
      @Override
      public <T> URIO<R, T> apply(Kind<PureIO<R, Void, ?>, ? extends T> from) {
        return new URIO<>(from.fix(PureIOOf::toPureIO));
      }
    })));
  }

  @Override
  public URIO<R, A> timeout(Duration duration) {
    return timeout(Future.DEFAULT_EXECUTOR, duration);
  }

  public URIO<R, A> timeout(Executor executor, Duration duration) {
    return racePair(executor, this, sleep(duration)).flatMap(either -> either.fold(
        ta -> ta.get2().cancel().fix(URIOOf::toURIO).map(x -> ta.get1()),
        tb -> tb.get1().cancel().fix(URIOOf::toURIO).flatMap(x -> URIO.raiseError(new TimeoutException()))));
  }

  @Override
  public URIO<R, A> repeat() {
    return repeat(1);
  }

  @Override
  public URIO<R, A> repeat(int times) {
    return repeat(this, unit(), times);
  }

  @Override
  public URIO<R, A> repeat(Duration delay) {
    return repeat(delay, 1);
  }

  @Override
  public URIO<R, A> repeat(Duration delay, int times) {
    return repeat(this, sleep(delay), times);
  }

  @Override
  public URIO<R, A> retry() {
    return retry(1);
  }

  @Override
  public URIO<R, A> retry(int maxRetries) {
    return retry(this, unit(), maxRetries);
  }

  @Override
  public URIO<R, A> retry(Duration delay) {
    return retry(delay, 1);
  }

  @Override
  public URIO<R, A> retry(Duration delay, int maxRetries) {
    return retry(this, sleep(delay), maxRetries);
  }

  @Override
  public URIO<R, Tuple2<Duration, A>> timed() {
    return new URIO<>(instance.timed());
  }

  public static <R> URIO<R, Unit> forked(Executor executor) {
    return async((env, callback) -> executor.execute(() -> callback.accept(Try.success(Unit.unit()))));
  }

  public static <R, A> URIO<R, A> accessM(Function1<? super R, ? extends Kind<URIO<R, ?>, ? extends A>> map) {
    return new URIO<>(PureIO.accessM(map.andThen(URIOOf::toURIO).andThen(URIO::toPureIO)));
  }

  public static <R, A> URIO<R, A> access(Function1<? super R, ? extends A> map) {
    return accessM(map.andThen(URIO::pure));
  }

  public static <R> URIO<R, R> env() {
    return access(identity());
  }

  public static <R, A, B, C> URIO<R, C> parMap2(Kind<URIO<R, ?>, ? extends A> za, Kind<URIO<R, ?>, ? extends B> zb,
      Function2<? super A, ? super B, ? extends C> mapper) {
    return parMap2(Future.DEFAULT_EXECUTOR, za, zb, mapper);
  }

  public static <R, A, B, C> URIO<R, C> parMap2(Executor executor, Kind<URIO<R, ?>, ? extends A> za, Kind<URIO<R, ?>, ? extends B> zb,
      Function2<? super A, ? super B, ? extends C> mapper) {
    return new URIO<>(PureIO.parMap2(executor, za.fix(URIOOf::toURIO).instance, zb.fix(URIOOf::toURIO).instance, mapper));
  }

  public static <R, A, B> URIO<R, Either<A, B>> race(Kind<URIO<R, ?>, ? extends A> fa, Kind<URIO<R, ?>, ? extends B> fb) {
    return race(Future.DEFAULT_EXECUTOR, fa, fb);
  }

  public static <R, A, B> URIO<R, Either<A, B>> race(Executor executor, Kind<URIO<R, ?>, ? extends A> fa, Kind<URIO<R, ?>, ? extends B> fb) {
    return racePair(executor, fa, fb).flatMap(either -> either.fold(
        ta -> ta.get2().cancel().fix(URIOOf::toURIO).map(x -> Either.left(ta.get1())),
        tb -> tb.get1().cancel().fix(URIOOf::toURIO).map(x -> Either.right(tb.get2()))));
  }

  public static <R, A, B> URIO<R, Either<Tuple2<A, Fiber<URIO<R, ?>, B>>, Tuple2<Fiber<URIO<R, ?>, A>, B>>>
      racePair(Executor executor, Kind<URIO<R, ?>, ? extends A> fa, Kind<URIO<R, ?>, ? extends B> fb) {
    PureIO<R, Void, A> instance1 = fa.fix(URIOOf::toURIO).instance.fix(PureIOOf::toPureIO);
    PureIO<R, Void, B> instance2 = fb.fix(URIOOf::toURIO).instance.fix(PureIOOf::toPureIO);
    return new URIO<>(PureIO.racePair(executor, instance1, instance2).map(
      either -> either.bimap(a -> a.map2(f -> f.<URIO<R, ?>>mapK(new FunctionK<>() {
        @Override
        public <T> URIO<R, T> apply(Kind<PureIO<R, Void, ?>, ? extends T> from) {
          return new URIO<>(from.fix(PureIOOf::toPureIO));
        }
      })), b -> b.map1(f -> f.<URIO<R, ?>>mapK(new FunctionK<>() {
        @Override
        public <T> URIO<R, T> apply(Kind<PureIO<R, Void, ?>, ? extends T> from) {
          return new URIO<>(from.fix(PureIOOf::toPureIO));
        }
      })))));
  }

  public static <R, A, B> Function1<A, URIO<R, B>> lift(Function1<? super A, ? extends B> function) {
    return value -> task(() -> function.apply(value));
  }

  public static <R, A, B> Function1<A, URIO<R, B>> liftOption(Function1<? super A, ? extends Option<? extends B>> function) {
    return value -> fromOption(function.apply(value));
  }

  public static <R, A, B> Function1<A, URIO<R, B>> liftTry(Function1<? super A, ? extends Try<? extends B>> function) {
    return value -> fromTry(function.apply(value));
  }

  public static <R, A, B> Function1<A, URIO<R, B>> liftEither(Function1<? super A, ? extends Either<Throwable, ? extends B>> function) {
    return value -> fromEither(function.apply(value));
  }

  public static <R> URIO<R, Unit> sleep(Duration delay) {
    return sleep(Future.DEFAULT_EXECUTOR, delay);
  }

  public static <R> URIO<R, Unit> sleep(Executor executor, Duration delay) {
    return fold(PureIO.sleep(executor, delay));
  }

  public static <R> URIO<R, Unit> exec(CheckedRunnable task) {
    return fold(PureIO.exec(task));
  }

  public static <R, A> URIO<R, A> pure(A value) {
    return new URIO<>(PureIO.pure(value));
  }

  public static <R, A> URIO<R, A> raiseError(Throwable throwable) {
    return new URIO<>(PureIO.fromEither(() -> { throw throwable; }));
  }

  public static <R, A> URIO<R, A> defer(Producer<Kind<URIO<R, ?>, ? extends A>> lazy) {
    return new URIO<>(PureIO.defer(() -> lazy.andThen(URIOOf::toURIO).get().instance));
  }

  public static <R, A> URIO<R, A> task(Producer<? extends A> task) {
    return fold(PureIO.task(task));
  }

  public static <R, T> URIO<R, T> fromOption(Option<? extends T> task) {
    return fromEither(task.toEither());
  }

  public static <R, T> URIO<R, T> fromTry(Try<? extends T> task) {
    return fromEither(task.toEither());
  }

  public static <R, T> URIO<R, T> fromEither(Either<Throwable, ? extends T> task) {
    return task.fold(URIO::raiseError, URIO::pure);
  }

  public static <R, A> URIO<R, A> never() {
    return async((env, cb) -> {});
  }

  public static <R, A> URIO<R, A> async(Consumer2<R, Consumer1<? super Try<? extends A>>> consumer) {
    return fold(PureIO.async(
        (env, cb1) -> consumer.accept(env, result -> cb1.accept(result.map(Either::right)))));
  }

  public static <R, A> URIO<R, A> cancellable(Function2<R, Consumer1<? super Try<? extends A>>, URIO<R, Unit>> consumer) {
    return fold(PureIO.cancellable(
        (env, cb1) -> consumer.andThen(URIO::<Throwable>toPureIO).apply(env, result -> cb1.accept(result.map(Either::right)))));
  }

  public static <R, A> URIO<R, Sequence<A>> traverse(Sequence<? extends Kind<URIO<R, ?>, A>> sequence) {
    return traverse(Future.DEFAULT_EXECUTOR, sequence);
  }

  public static <R, A> URIO<R, Sequence<A>> traverse(Executor executor, Sequence<? extends Kind<URIO<R, ?>, A>> sequence) {
    return sequence.foldLeft(pure(ImmutableList.empty()),
        (Kind<URIO<R, ?>, Sequence<A>> xs, Kind<URIO<R, ?>, A> a) -> parMap2(executor, xs, a, Sequence::append));
  }

  public static <R, A extends AutoCloseable, B> URIO<R, B> bracket(
    Kind<URIO<R, ?>, ? extends A> acquire, Function1<? super A, ? extends Kind<URIO<R, ?>, ? extends B>> use) {
    return fold(PureIO.bracket(PureIO.redeem(acquire.fix(URIOOf::toURIO).instance),
        resource -> PureIO.redeem(use.andThen(URIOOf::toURIO).apply(resource).instance)));
  }

  public static <R, A, B> URIO<R, B> bracket(Kind<URIO<R, ?>, ? extends A> acquire,
      Function1<? super A, ? extends Kind<URIO<R, ?>, ? extends B>> use, Consumer1<? super A> release) {
    return fold(PureIO.bracket(PureIO.redeem(acquire.fix(URIOOf::toURIO).instance),
        resource -> PureIO.redeem(use.andThen(URIOOf::toURIO).apply(resource).instance), release));
  }

  public static <R, A, B> URIO<R, B> bracket(Kind<URIO<R, ?>, ? extends A> acquire,
      Function1<? super A, ? extends Kind<URIO<R, ?>, ? extends B>> use, Function1<? super A, ? extends Kind<URIO<R, ?>, Unit>> release) {
    return fold(PureIO.bracket(PureIO.redeem(acquire.fix(URIOOf::toURIO).instance),
        resource -> PureIO.redeem(use.andThen(URIOOf::toURIO).apply(resource).instance), release.andThen(URIOOf::toURIO).andThen(URIO::toPureIO)));
  }

  @SuppressWarnings("unchecked")
  public static <R> URIO<R, Unit> unit() {
    return (URIO<R, Unit>) UNIT;
  }

  private static <R, A> URIO<R, A> fold(PureIO<R, Throwable, A> zio) {
    return new URIO<>(zio.foldM(error -> URIO.<R, A>raiseError(error).instance, value -> URIO.<R, A>pure(value).instance));
  }

  private static <R, A> URIO<R, A> repeat(URIO<R, A> self, URIO<R, Unit> pause, int times) {
    return self.redeemWith(URIO::raiseError, value -> {
      if (times > 0) {
        return pause.andThen(repeat(self, pause, times - 1));
      }
      return URIO.pure(value);
    });
  }

  private static <R, A> URIO<R, A> retry(URIO<R, A> self, URIO<R, Unit> pause, int maxRetries) {
    return self.redeemWith(error -> {
      if (maxRetries > 0) {
        return pause.andThen(retry(self, pause.repeat(), maxRetries - 1));
      }
      return URIO.raiseError(error);
    }, URIO::pure);
  }
}
