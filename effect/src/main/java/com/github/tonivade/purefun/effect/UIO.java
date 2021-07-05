/*
 * Copyright (c) 2018-2021, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
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
import java.util.concurrent.TimeoutException;

import com.github.tonivade.purefun.CheckedRunnable;
import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Effect;
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
import com.github.tonivade.purefun.concurrent.Future;
import com.github.tonivade.purefun.data.ImmutableList;
import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.type.Try;
import com.github.tonivade.purefun.typeclasses.Fiber;
import com.github.tonivade.purefun.typeclasses.FunctionK;

@HigherKind
public final class UIO<A> implements UIOOf<A>, Effect<UIO_, A>, Recoverable {

  private static final UIO<Unit> UNIT = new UIO<>(ZIO.unit());

  private final ZIO<Nothing, Nothing, A> instance;

  UIO(ZIO<Nothing, Nothing, A> value) {
    this.instance = checkNonNull(value);
  }
  
  public Future<A> runAsync() {
    return instance.runAsync(nothing()).map(Either::getRight);
  }

  public A unsafeRunSync() {
    return instance.provide(nothing()).get();
  }

  public Try<A> safeRunSync() {
    return Try.of(this::unsafeRunSync);
  }

  @SuppressWarnings("unchecked")
  public <R, E> ZIO<R, E, A> toZIO() {
    return (ZIO<R, E, A>) instance;
  }

  @SuppressWarnings("unchecked")
  public <E> EIO<E, A> toEIO() {
    return new EIO<>((ZIO<Nothing, E, A>) instance);
  }

  @SuppressWarnings("unchecked")
  public <R> RIO<R, A> toRIO() {
    return new RIO<>((ZIO<R, Throwable, A>) ZIO.redeem(instance));
  }

  @SuppressWarnings("unchecked")
  public <R> URIO<R, A> toURIO() {
    return new URIO<>((ZIO<R, Nothing, A>) instance);
  }

  public Task<A> toTask() {
    return new Task<>(ZIO.redeem(instance));
  }

  public void safeRunAsync(Consumer1<? super Try<? extends A>> callback) {
    instance.provideAsync(nothing(), x -> callback.accept(x.map(Either::getRight)));
  }

  @Override
  public <B> UIO<B> map(Function1<? super A, ? extends B> map) {
    return new UIO<>(instance.map(map));
  }

  @Override
  public <B> UIO<B> flatMap(Function1<? super A, ? extends Kind<UIO_, ? extends B>> map) {
    return new UIO<>(instance.flatMap(x -> {
      UIO<? extends B> apply = map.andThen(UIOOf::narrowK).apply(x);
      return apply.instance;
    }));
  }

  @Override
  public <B> UIO<B> andThen(Kind<UIO_, ? extends B> next) {
    return new UIO<>(instance.andThen(next.fix(UIOOf.toUIO()).instance));
  }

  @Override
  public <B> UIO<B> ap(Kind<UIO_, Function1<? super A, ? extends B>> apply) {
    return new UIO<>(instance.ap(apply.fix(UIOOf.toUIO()).instance));
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
      Function1<? super Throwable, ? extends Kind<UIO_, ? extends B>> mapError, 
      Function1<? super A, ? extends Kind<UIO_, ? extends B>> map) {
    return new UIO<>(ZIO.redeem(instance).foldM(
        error -> mapError.andThen(UIOOf::narrowK).apply(error).instance, 
        value -> map.andThen(UIOOf::narrowK).apply(value).instance));
  }
  
  @Override
  public <B> UIO<Tuple2<A, B>> zip(Kind<UIO_, ? extends B> other) {
    return zipWith(other, Tuple::of);
  }
  
  @Override
  public <B> UIO<A> zipLeft(Kind<UIO_, ? extends B> other) {
    return zipWith(other, first());
  }
  
  @Override
  public <B> UIO<B> zipRight(Kind<UIO_, ? extends B> other) {
    return zipWith(other, second());
  }
  
  @Override
  public <B, C> UIO<C> zipWith(Kind<UIO_, ? extends B> other, 
      Function2<? super A, ? super B, ? extends C> mapper) {
    return parMap2(this, other.fix(UIOOf.toUIO()), mapper);
  }
  
  public UIO<Fiber<UIO_, A>> fork() {
    return new UIO<>(instance.fork().map(f -> f.mapK(new FunctionK<Kind<Kind<ZIO_, Nothing>, Nothing>, UIO_>() {
      @Override
      public <T> UIO<T> apply(Kind<Kind<Kind<ZIO_, Nothing>, Nothing>, ? extends T> from) {
        return new UIO<>(from.fix(ZIOOf::narrowK));
      }
    })));
  }

  public UIO<A> timeout(Duration duration) {
    return timeout(Future.DEFAULT_EXECUTOR, duration);
  }
  
  public UIO<A> timeout(Executor executor, Duration duration) {
    return racePair(executor, this, sleep(duration)).flatMap(either -> either.fold(
        ta -> ta.get2().cancel().fix(UIOOf.toUIO()).map(x -> ta.get1()),
        tb -> tb.get1().cancel().fix(UIOOf.toUIO()).flatMap(x -> UIO.raiseError(new TimeoutException()))));
  }

  @Override
  public UIO<A> repeat() {
    return repeat(1);
  }

  @Override
  public UIO<A> repeat(int times) {
    return fold(ZIO.redeem(instance).repeat(times));
  }

  @Override
  public UIO<A> repeat(Duration delay) {
    return repeat(delay, 1);
  }

  @Override
  public UIO<A> repeat(Duration delay, int times) {
    return fold(ZIO.redeem(instance).repeat(delay, times));
  }
  
  public <B> UIO<B> repeat(Schedule<Nothing, A, B> schedule) {
    return fold(ZIO.redeem(instance).repeat(schedule));
  }

  @Override
  public UIO<A> retry() {
    return retry(1);
  }

  @Override
  public UIO<A> retry(int maxRetries) {
    return retry(Schedule.recurs(maxRetries));
  }

  @Override
  public UIO<A> retry(Duration delay) {
    return retry(delay, 1);
  }

  @Override
  public UIO<A> retry(Duration delay, int maxRetries) {
    return retry(Schedule.<Nothing, Throwable>recursSpaced(delay, maxRetries));
  }
  
  public <B> UIO<A> retry(Schedule<Nothing, Throwable, B> schedule) {
    return fold(ZIO.redeem(instance).retry(schedule));
  }

  @Override
  public UIO<Tuple2<Duration, A>> timed() {
    return new UIO<>(instance.timed());
  }
  
  public static UIO<Unit> forked(Executor executor) {
    return async(callback -> executor.execute(() -> callback.accept(Try.success(Unit.unit()))));
  }

  public static <A, B, C> UIO<C> parMap2(UIO<? extends A> za, UIO<? extends B> zb, 
      Function2<? super A, ? super B, ? extends C> mapper) {
    return parMap2(Future.DEFAULT_EXECUTOR, za, zb, mapper);
  }

  public static <A, B, C> UIO<C> parMap2(Executor executor, UIO<? extends A> za, UIO<? extends B> zb, 
      Function2<? super A, ? super B, ? extends C> mapper) {
    return new UIO<>(ZIO.parMap2(executor, za.instance, zb.instance, mapper));
  }
  
  public static <A, B> UIO<Either<A, B>> race(Kind<UIO_, A> fa, Kind<UIO_, B> fb) {
    return race(Future.DEFAULT_EXECUTOR, fa, fb);
  }
  
  public static <A, B> UIO<Either<A, B>> race(Executor executor, Kind<UIO_, A> fa, Kind<UIO_, B> fb) {
    return racePair(executor, fa, fb).flatMap(either -> either.fold(
        ta -> ta.get2().cancel().fix(UIOOf.toUIO()).map(x -> Either.left(ta.get1())),
        tb -> tb.get1().cancel().fix(UIOOf.toUIO()).map(x -> Either.right(tb.get2()))));
  }
  
  public static <A, B> UIO<Either<Tuple2<A, Fiber<UIO_, B>>, Tuple2<Fiber<UIO_, A>, B>>> 
      racePair(Executor executor, Kind<UIO_, A> fa, Kind<UIO_, B> fb) {
    ZIO<Nothing, Nothing, A> instance1 = fa.fix(UIOOf.toUIO()).instance;
    ZIO<Nothing, Nothing, B> instance2 = fb.fix(UIOOf.toUIO()).instance;
    return new UIO<>(ZIO.racePair(executor, instance1, instance2).map(
      either -> either.bimap(a -> a.map2(f -> f.mapK(new FunctionK<Kind<Kind<ZIO_, Nothing>, Nothing>, UIO_>() {
        @Override
        public <T> UIO<T> apply(Kind<Kind<Kind<ZIO_, Nothing>, Nothing>, ? extends T> from) {
          return new UIO<>(from.fix(ZIOOf::narrowK));
        }
      })), b -> b.map1(f -> f.mapK(new FunctionK<Kind<Kind<ZIO_, Nothing>, Nothing>, UIO_>() {
        @Override
        public <T> UIO<T> apply(Kind<Kind<Kind<ZIO_, Nothing>, Nothing>, ? extends T> from) {
          return new UIO<>(from.fix(ZIOOf::narrowK));
        }
      })))));
  }

  public static <A, B> Function1<A, UIO<B>> lift(Function1<? super A, ? extends B> function) {
    return value -> task(() -> function.apply(value));
  }

  public static <A, B> Function1<A, UIO<B>> liftOption(Function1<? super A, Option<? extends B>> function) {
    return value -> fromOption(function.apply(value));
  }

  public static <A, B> Function1<A, UIO<B>> liftTry(Function1<? super A, Try<? extends B>> function) {
    return value -> fromTry(function.apply(value));
  }

  public static <A, B> Function1<A, UIO<B>> liftEither(Function1<? super A, Either<Throwable, ? extends B>> function) {
    return value -> fromEither(function.apply(value));
  }

  public static UIO<Unit> sleep(Duration delay) {
    return fold(ZIO.sleep(delay));
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

  public static <A> UIO<A> defer(Producer<UIO<? extends A>> lazy) {
    return new UIO<>(ZIO.defer(() -> lazy.get().instance));
  }

  public static <A> UIO<A> task(Producer<? extends A> task) {
    return fold(ZIO.task(task));
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
  
  public static <A> UIO<A> never() {
    return async(cb -> {});
  }
  
  public static <A> UIO<A> async(Consumer1<Consumer1<? super Try<? extends A>>> consumer) {
    return fold(ZIO.async(
        (env, cb1) -> consumer.accept(result -> cb1.accept(result.map(Either::right)))));
  }
  
  public static <A> UIO<A> cancellable(Function1<Consumer1<? super Try<? extends A>>, UIO<Unit>> consumer) {
    return fold(ZIO.cancellable(
        (env, cb1) -> consumer.andThen(UIO::<Nothing, Throwable>toZIO).apply(result -> cb1.accept(result.map(Either::right)))));
  }

  public static <A> UIO<Sequence<A>> traverse(Sequence<? extends UIO<A>> sequence) {
    return traverse(Future.DEFAULT_EXECUTOR, sequence);
  }

  public static <A> UIO<Sequence<A>> traverse(Executor executor, Sequence<? extends UIO<A>> sequence) {
    return sequence.foldLeft(pure(ImmutableList.empty()), 
        (UIO<Sequence<A>> xs, UIO<A> a) -> parMap2(executor, xs, a, Sequence::append));
  }

  public static <A extends AutoCloseable, B> UIO<B> bracket(
      UIO<? extends A> acquire, Function1<? super A, ? extends UIO<? extends B>> use) {
    return fold(ZIO.bracket(ZIO.redeem(acquire.instance), 
        resource -> ZIO.redeem(use.andThen(UIOOf::narrowK).apply(resource).instance)));
  }

  public static <A, B> UIO<B> bracket(UIO<? extends A> acquire, 
      Function1<? super A, ? extends UIO<? extends B>> use, Consumer1<? super A> release) {
    return fold(ZIO.bracket(ZIO.redeem(acquire.instance), 
        resource -> ZIO.redeem(use.andThen(UIOOf::narrowK).apply(resource).instance), release));
  }

  public static <A, B> UIO<B> bracket(UIO<? extends A> acquire, 
      Function1<? super A, ? extends UIO<? extends B>> use, Function1<? super A, ? extends UIO<Unit>> release) {
    return fold(ZIO.bracket(ZIO.redeem(acquire.instance), 
        resource -> ZIO.redeem(use.andThen(UIOOf::narrowK).apply(resource).instance), release.andThen(UIO::toZIO)));
  }

  public static UIO<Unit> unit() {
    return UNIT;
  }

  private static <A> UIO<A> fold(ZIO<Nothing, Throwable, A> zio) {
    return new UIO<>(zio.foldM(error -> UIO.<A>raiseError(error).instance, value -> UIO.pure(value).instance));
  }
}
