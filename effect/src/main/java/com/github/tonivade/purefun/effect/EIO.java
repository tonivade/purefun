/*
 * Copyright (c) 2018-2025, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
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
public final class EIO<E, A> implements EIOOf<E, A>, Effect<EIO<E, ?>, A> {

  private static final EIO<?, Unit> UNIT = new EIO<>(PureIO.unit());

  private final PureIO<Void, E, A> instance;

  EIO(PureIO<Void, E, A> value) {
    this.instance = checkNonNull(value);
  }

  @SuppressWarnings("unchecked")
  public <R> PureIO<R, E, A> toPureIO() {
    return (PureIO<R, E, A>) instance;
  }

  public UIO<A> toUIO() {
    return new UIO<>(instance.toURIO().toPureIO());
  }

  public Either<E, A> safeRunSync() {
    return instance.provide(null);
  }

  public Future<Either<E, A>> runAsync() {
    return instance.runAsync(null);
  }

  public Future<Either<E, A>> runAsync(Executor executor) {
    return EIO.<E>forked(executor).andThen(this).runAsync();
  }

  public void safeRunAsync(Consumer1<? super Try<? extends Either<E, ? extends A>>> callback) {
    instance.provideAsync(null, callback);
  }

  @Override
  public <B> EIO<E, B> map(Function1<? super A, ? extends B> map) {
    return new EIO<>(instance.map(map));
  }

  @Override
  public <B> EIO<E, B> flatMap(Function1<? super A, ? extends Kind<EIO<E, ?>, ? extends B>> map) {
    return new EIO<>(instance.flatMap(value -> {
      EIO<E, ? extends B> apply = map.andThen(EIOOf::toEIO).apply(value);
      return apply.instance;
    }));
  }

  @Override
  public <B> EIO<E, B> andThen(Kind<EIO<E, ?>, ? extends B> next) {
    return new EIO<>(instance.andThen(next.fix(EIOOf::toEIO).instance));
  }

  @Override
  public <B> EIO<E, B> ap(Kind<EIO<E, ?>, ? extends Function1<? super A, ? extends B>> apply) {
    return new EIO<>(instance.ap(apply.fix(EIOOf::toEIO).toPureIO()));
  }

  public EIO<A, E> swap() {
    return new EIO<>(instance.swap());
  }

  public <B> EIO<B, A> mapError(Function1<? super E, ? extends B> map) {
    return new EIO<>(instance.mapError(map));
  }

  public <F> EIO<F, A> flatMapError(Function1<? super E, ? extends Kind<EIO<F, ?>, ? extends A>> map) {
    return new EIO<>(instance.flatMapError(error -> {
      EIO<F, ? extends A> apply = map.andThen(EIOOf::toEIO).apply(error);
      return apply.instance;
    }));
  }

  public <B, F> EIO<F, B> bimap(Function1<? super E, ? extends F> mapError, Function1<? super A, ? extends B> map) {
    return new EIO<>(instance.bimap(mapError, map));
  }

  public <B, F> EIO<F, B> foldM(
      Function1<? super E, ? extends Kind<EIO<F, ?>, ? extends B>> mapError,
      Function1<? super A, ? extends Kind<EIO<F, ?>, ? extends B>> map) {
    return new EIO<>(instance.foldM(
        error -> mapError.andThen(EIOOf::toEIO).apply(error).instance,
        value -> map.andThen(EIOOf::toEIO).apply(value).instance));
  }

  public <B> UIO<B> fold(Function1<? super E, ? extends B> mapError, Function1<? super A, ? extends B> map) {
    return new UIO<>(instance.fold(mapError, map).toPureIO());
  }

  public UIO<A> recover(Function1<? super E, ? extends A> mapError) {
    return new UIO<>(instance.recover(mapError).toPureIO());
  }

  public EIO<E, A> orElse(EIO<E, ? extends A> other) {
    return new EIO<>(instance.orElse(other.instance));
  }

  @Override
  public <B> EIO<E, Tuple2<A, B>> zip(Kind<EIO<E, ?>, ? extends B> other) {
    return zipWith(other, Tuple::of);
  }

  @Override
  public <B> EIO<E, A> zipLeft(Kind<EIO<E, ?>, ? extends B> other) {
    return zipWith(other, first());
  }

  @Override
  public <B> EIO<E, B> zipRight(Kind<EIO<E, ?>, ? extends B> other) {
    return zipWith(other, second());
  }

  @Override
  public <B, C> EIO<E, C> zipWith(Kind<EIO<E, ?>, ? extends B> other,
      Function2<? super A, ? super B, ? extends C> mapper) {
    return parMap2(this, other.fix(EIOOf::toEIO), mapper);
  }

  public EIO<E, Fiber<EIO<E, ?>, A>> fork() {
    return new EIO<>(instance.fork().map(f -> f.<EIO<E, ?>>mapK(new FunctionK<>() {
      @Override
      public <T> EIO<E, T> apply(Kind<PureIO<Void, E, ?>, ? extends T> from) {
        return new EIO<>(from.fix(PureIOOf::toPureIO));
      }
    })));
  }

  @Override
  public EIO<E, A> timeout(Duration duration) {
    return timeout(Future.DEFAULT_EXECUTOR, duration);
  }

  public EIO<E, A> timeout(Executor executor, Duration duration) {
    return racePair(executor, this, EIO.sleep(duration)).flatMap(either -> either.fold(
        ta -> ta.get2().cancel().fix(EIOOf::toEIO).map(x -> ta.get1()),
        tb -> tb.get1().cancel().fix(EIOOf::toEIO).flatMap(x -> EIO.throwError(new TimeoutException()))));
  }

  @Override
  public EIO<E, A> repeat() {
    return repeat(1);
  }

  @Override
  public EIO<E, A> repeat(int times) {
    return new EIO<>(instance.repeat(times));
  }

  @Override
  public EIO<E, A> repeat(Duration delay) {
    return repeat(delay, 1);
  }

  @Override
  public EIO<E, A> repeat(Duration delay, int times) {
    return new EIO<>(instance.repeat(delay, times));
  }

  @Override
  public EIO<E, A> retry() {
    return retry(1);
  }

  @Override
  public EIO<E, A> retry(int maxRetries) {
    return new EIO<>(instance.retry(maxRetries));
  }

  @Override
  public EIO<E, A> retry(Duration delay) {
    return retry(delay, 1);
  }

  @Override
  public EIO<E, A> retry(Duration delay, int maxRetries) {
    return new EIO<>(instance.retry(delay, maxRetries));
  }

  @Override
  public EIO<E, Tuple2<Duration, A>> timed() {
    return new EIO<>(instance.timed());
  }

  public static <E> EIO<E, Unit> forked(Executor executor) {
    return async(callback -> executor.execute(() -> callback.accept(Try.success(Either.right(Unit.unit())))));
  }

  public <X extends Throwable> EIO<X, A> refineOrDie(Class<X> type) {
    return new EIO<>(instance.refineOrDie(type));
  }

  public static <E, A, B, C> EIO<E, C> parMap2(Kind<EIO<E, ?>, ? extends A> za, Kind<EIO<E, ?>, ? extends B> zb,
      Function2<? super A, ? super B, ? extends C> mapper) {
    return parMap2(Future.DEFAULT_EXECUTOR, za, zb, mapper);
  }

  public static <E, A, B, C> EIO<E, C> parMap2(Executor executor, Kind<EIO<E, ?>, ? extends A> za, Kind<EIO<E, ?>, ? extends B> zb,
      Function2<? super A, ? super B, ? extends C> mapper) {
    return new EIO<>(PureIO.parMap2(executor, za.fix(EIOOf::toEIO).instance, zb.fix(EIOOf::toEIO).instance, mapper));
  }

  public static <E, A, B> EIO<E, Either<A, B>> race(Kind<EIO<E, ?>, ? extends A> fa, Kind<EIO<E, ?>, ? extends B> fb) {
    return race(Future.DEFAULT_EXECUTOR, fa, fb);
  }

  public static <E, A, B> EIO<E, Either<A, B>> race(Executor executor, Kind<EIO<E, ?>, ? extends A> fa, Kind<EIO<E, ?>, ? extends B> fb) {
    return racePair(executor, fa, fb).flatMap(either -> either.fold(
        ta -> ta.get2().cancel().fix(EIOOf::toEIO).map(x -> Either.left(ta.get1())),
        tb -> tb.get1().cancel().fix(EIOOf::toEIO).map(x -> Either.right(tb.get2()))));
  }

  public static <E, A, B> EIO<E, Either<Tuple2<A, Fiber<EIO<E, ?>, B>>, Tuple2<Fiber<EIO<E, ?>, A>, B>>>
      racePair(Executor executor, Kind<EIO<E, ?>, ? extends A> fa, Kind<EIO<E, ?>, ? extends B> fb) {
    PureIO<Void, E, A> instance1 = fa.fix(EIOOf::toEIO).instance.fix(PureIOOf::toPureIO);
    PureIO<Void, E, B> instance2 = fb.fix(EIOOf::toEIO).instance.fix(PureIOOf::toPureIO);
    return new EIO<>(PureIO.racePair(executor, instance1, instance2).map(
      either -> either.bimap(a -> a.map2(f -> f.<EIO<E, ?>>mapK(new FunctionK<>() {
        @Override
        public <T> EIO<E, T> apply(Kind<PureIO<Void, E, ?>, ? extends T> from) {
          return new EIO<>(from.fix(PureIOOf::toPureIO));
        }
      })), b -> b.map1(f -> f.<EIO<E, ?>>mapK(new FunctionK<>() {
        @Override
        public <T> EIO<E, T> apply(Kind<PureIO<Void, E, ?>, ? extends T> from) {
          return new EIO<>(from.fix(PureIOOf::toPureIO));
        }
      })))));
  }

  public static <E, A> EIO<E, A> absorb(EIO<E, Either<E, A>> value) {
    return new EIO<>(PureIO.absorb(value.instance));
  }

  public static <A, B> Function1<A, EIO<Throwable, B>> lift(Function1<? super A, ? extends B> function) {
    return PureIO.<Void, A, B>lift(function).andThen(EIO::new);
  }

  public static <A, B> Function1<A, EIO<Throwable, B>> liftOption(Function1<? super A, ? extends Option<? extends B>> function) {
    return value -> fromOption(function.apply(value));
  }

  public static <A, B> Function1<A, EIO<Throwable, B>> liftTry(Function1<? super A, ? extends Try<? extends B>> function) {
    return value -> fromTry(function.apply(value));
  }

  public static <E, A, B> Function1<A, EIO<E, B>> liftEither(Function1<? super A, ? extends Either<E, ? extends B>> function) {
    return value -> fromEither(function.apply(value));
  }

  public static <A> EIO<Throwable, A> fromOption(Option<? extends A> task) {
    return fromOption(cons(task));
  }

  public static <A> EIO<Throwable, A> fromOption(Producer<Option<? extends A>> task) {
    return new EIO<>(PureIO.fromOption(task));
  }

  public static <A> EIO<Throwable, A> fromTry(Try<? extends A> task) {
    return fromTry(cons(task));
  }

  public static <A> EIO<Throwable, A> fromTry(Producer<Try<? extends A>> task) {
    return new EIO<>(PureIO.fromTry(task));
  }

  public static <E, A> EIO<E, A> fromEither(Either<E, ? extends A> task) {
    return fromEither(cons(task));
  }

  public static <E, A> EIO<E, A> fromEither(Producer<Either<E, ? extends A>> task) {
    return new EIO<>(PureIO.fromEither(task));
  }

  public static <E> EIO<E, Unit> sleep(Duration delay) {
    return sleep(Future.DEFAULT_EXECUTOR, delay);
  }

  public static <E> EIO<E, Unit> sleep(Executor executor, Duration delay) {
    return new EIO<>(PureIO.sleep(executor, delay));
  }

  public static EIO<Throwable, Unit> exec(CheckedRunnable task) {
    return new EIO<>(PureIO.exec(task));
  }

  public static <E, A> EIO<E, A> pure(A value) {
    return new EIO<>(PureIO.pure(value));
  }

  public static <E, A> EIO<E, A> defer(Producer<Kind<EIO<E, ?>, ? extends A>> lazy) {
    return new EIO<>(PureIO.defer(() -> lazy.andThen(EIOOf::toEIO).get().instance));
  }

  public static <A> EIO<Throwable, A> task(Producer<? extends A> task) {
    return new EIO<>(PureIO.task(task));
  }

  public static <E, A> EIO<E, A> never() {
    return async(cb -> {});
  }

  public static <E, A> EIO<E, A> async(Consumer1<Consumer1<? super Try<? extends Either<E, ? extends A>>>> consumer) {
    return new EIO<>(PureIO.async((env, cb) -> consumer.accept(cb)));
  }

  public static <E, A> EIO<E, A> cancellable(Function1<Consumer1<? super Try<? extends Either<E, ? extends A>>>, EIO<E, Unit>> consumer) {
    return new EIO<>(PureIO.cancellable((env, cb) -> consumer.andThen(EIO::<Void>toPureIO).apply(cb)));
  }

  public static <E, A> EIO<E, A> raiseError(E error) {
    return new EIO<>(PureIO.raiseError(error));
  }

  public static <E, A> EIO<E, A> throwError(Throwable error) {
    return new EIO<>(PureIO.throwError(error));
  }

  public static <E, A> EIO<E, Sequence<A>> traverse(Sequence<? extends Kind<EIO<E, ?>, ? extends A>> sequence) {
    return traverse(Future.DEFAULT_EXECUTOR, sequence);
  }

  public static <E, A> EIO<E, Sequence<A>> traverse(Executor executor, Sequence<? extends Kind<EIO<E, ?>, ? extends A>> sequence) {
    return sequence.foldLeft(pure(ImmutableList.empty()),
        (Kind<EIO<E, ?>, ? extends Sequence<A>> xs, Kind<EIO<E, ?>, ? extends A> a) -> parMap2(executor, xs, a, Sequence::append));
  }

  public static <E, A extends AutoCloseable, B> EIO<E, B> bracket(Kind<EIO<E, ?>, ? extends A> acquire,
      Function1<? super A, ? extends Kind<EIO<E, ?>, ? extends B>> use) {
    return new EIO<>(PureIO.bracket(acquire.fix(EIOOf::toEIO).instance,
        resource -> use.andThen(EIOOf::<E, B>toEIO).apply(resource).instance));
  }

  public static <E, A, B> EIO<E, B> bracket(Kind<EIO<E, ?>, ? extends A> acquire,
      Function1<? super A, ? extends Kind<EIO<E, ?>, ? extends B>> use, Consumer1<? super A> release) {
    return new EIO<>(PureIO.bracket(acquire.fix(EIOOf::toEIO).instance,
        resource -> use.andThen(EIOOf::<E, B>toEIO).apply(resource).instance, release));
  }

  public static <E, A, B> EIO<E, B> bracket(Kind<EIO<E, ?>, ? extends A> acquire,
      Function1<? super A, ? extends Kind<EIO<E, ?>, ? extends B>> use, Function1<? super A, ? extends Kind<EIO<E, ?>, Unit>> release) {
    return new EIO<>(PureIO.bracket(acquire.fix(EIOOf::toEIO).instance,
        resource -> use.andThen(EIOOf::<E, B>toEIO).apply(resource).instance, release.andThen(EIOOf::toEIO).andThen(EIO::toPureIO)));
  }

  @SuppressWarnings("unchecked")
  public static <E> EIO<E, Unit> unit() {
    return (EIO<E, Unit>) UNIT;
  }
}
