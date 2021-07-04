/*
 * Copyright (c) 2018-2021, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.effect;

import static com.github.tonivade.purefun.Function1.identity;
import static com.github.tonivade.purefun.Function2.first;
import static com.github.tonivade.purefun.Function2.second;
import static com.github.tonivade.purefun.Precondition.checkNonNull;

import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeoutException;
import com.github.tonivade.purefun.CheckedRunnable;
import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Consumer2;
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
public final class URIO<R, A> implements URIOOf<R, A>, Effect<Kind<URIO_, R>, A>, Recoverable {

  private static final URIO<?, Unit> UNIT = new URIO<>(ZIO.unit());

  private final ZIO<R, Nothing, A> instance;

  URIO(ZIO<R, Nothing, A> value) {
    this.instance = checkNonNull(value);
  }

  public A unsafeRunSync(R env) {
    return instance.provide(env).get();
  }

  public Try<A> safeRunSync(R env) {
    return Try.of(() -> unsafeRunSync(env));
  }

  @SuppressWarnings("unchecked")
  public <E> ZIO<R, E, A> toZIO() {
    return (ZIO<R, E, A>) instance;
  }

  @SuppressWarnings("unchecked")
  public <E> EIO<E, A> toEIO() {
    return new EIO<>((ZIO<Nothing, E, A>) instance);
  }

  public RIO<R, A> toRIO() {
    return new RIO<>(ZIO.redeem(instance));
  }

  public Future<A> runAsync(R env) {
    return instance.runAsync(env).map(Either::getRight);
  }

  public void safeRunAsync(R env, Consumer1<? super Try<? extends A>> callback) {
    instance.provideAsync(env, result -> callback.accept(result.map(Either::getRight)));
  }

  @Override
  public <B> URIO<R, B> map(Function1<? super A, ? extends B> map) {
    return new URIO<>(instance.map(map));
  }

  @Override
  public <B> URIO<R, B> flatMap(Function1<? super A, ? extends Kind<Kind<URIO_, R>, ? extends B>> map) {
    return new URIO<>(instance.flatMap(x -> {
      URIO<R, ? extends B> apply = map.andThen(URIOOf::narrowK).apply(x);
      return apply.instance;
    }));
  }

  @Override
  public <B> URIO<R, B> andThen(Kind<Kind<URIO_, R>, ? extends B> next) {
    return new URIO<>(instance.andThen(next.fix(URIOOf.toURIO()).instance));
  }
  
  @Override
  public <B> URIO<R, B> ap(Kind<Kind<URIO_, R>, Function1<? super A, ? extends B>> apply) {
    return new URIO<>(instance.ap(apply.fix(URIOOf.toURIO()).instance));
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
      Function1<? super Throwable, ? extends Kind<Kind<URIO_, R>, ? extends B>> mapError, 
      Function1<? super A, ? extends Kind<Kind<URIO_, R>, ? extends B>> map) {
    return new URIO<>(ZIO.redeem(instance).foldM(
        error -> mapError.andThen(URIOOf::narrowK).apply(error).instance, 
        value -> map.andThen(URIOOf::narrowK).apply(value).instance));
  }
  
  @Override
  public <B> URIO<R, Tuple2<A, B>> zip(Kind<Kind<URIO_, R>, ? extends B> other) {
    return zipWith(other, Tuple::of);
  }
  
  @Override
  public <B> URIO<R, A> zipLeft(Kind<Kind<URIO_, R>, ? extends B> other) {
    return zipWith(other, first());
  }
  
  @Override
  public <B> URIO<R, B> zipRight(Kind<Kind<URIO_, R>, ? extends B> other) {
    return zipWith(other, second());
  }
  
  @Override
  public <B, C> URIO<R, C> zipWith(Kind<Kind<URIO_, R>, ? extends B> other, 
      Function2<? super A, ? super B, ? extends C> mapper) {
    return parMap2(this, other.fix(URIOOf.toURIO()), mapper);
  }
  
  public URIO<R, Fiber<Kind<URIO_, R>, A>> fork() {
    return new URIO<>(instance.fork().map(f -> f.mapK(new FunctionK<Kind<Kind<ZIO_, R>, Nothing>, Kind<URIO_, R>>() {
      @Override
      public <T> URIO<R, T> apply(Kind<Kind<Kind<ZIO_, R>, Nothing>, ? extends T> from) {
        return new URIO<>(from.fix(ZIOOf::narrowK));
      }
    })));
  }

  public URIO<R, A> timeout(Duration duration) {
    return timeout(Future.DEFAULT_EXECUTOR, duration);
  }
  
  public URIO<R, A> timeout(Executor executor, Duration duration) {
    return racePair(executor, this, sleep(duration)).flatMap(either -> either.fold(
        ta -> ta.get2().cancel().fix(URIOOf.toURIO()).map(x -> ta.get1()),
        tb -> tb.get1().cancel().fix(URIOOf.toURIO()).flatMap(x -> URIO.raiseError(new TimeoutException()))));
  }

  @Override
  public URIO<R, A> repeat() {
    return repeat(1);
  }

  @Override
  public URIO<R, A> repeat(int times) {
    return fold(ZIO.redeem(instance).repeat(times));
  }

  @Override
  public URIO<R, A> repeat(Duration delay) {
    return repeat(delay, 1);
  }

  @Override
  public URIO<R, A> repeat(Duration delay, int times) {
    return fold(ZIO.redeem(instance).repeat(delay, times));
  }
  
  public <B> URIO<R, B> repeat(Schedule<R, A, B> schedule) {
    return fold(ZIO.redeem(instance).repeat(schedule));
  }

  @Override
  public URIO<R, A> retry() {
    return retry(1);
  }

  @Override
  public URIO<R, A> retry(int maxRetries) {
    return retry(Schedule.recurs(maxRetries));
  }

  @Override
  public URIO<R, A> retry(Duration delay) {
    return retry(delay, 1);
  }

  @Override
  public URIO<R, A> retry(Duration delay, int maxRetries) {
    return retry(Schedule.<R, Throwable>recursSpaced(delay, maxRetries));
  }
  
  public <B> URIO<R, A> retry(Schedule<R, Throwable, B> schedule) {
    return fold(ZIO.redeem(instance).retry(schedule));
  }

  @Override
  public URIO<R, Tuple2<Duration, A>> timed() {
    return new URIO<>(instance.timed());
  }
  
  public static <R> URIO<R, Unit> forked(Executor executor) {
    return async((env, callback) -> executor.execute(() -> callback.accept(Try.success(Unit.unit()))));
  }

  public static <R, A> URIO<R, A> accessM(Function1<? super R, ? extends URIO<R, ? extends A>> map) {
    return new URIO<>(ZIO.accessM(map.andThen(URIO::toZIO)));
  }

  public static <R, A> URIO<R, A> access(Function1<? super R, ? extends A> map) {
    return accessM(map.andThen(URIO::pure));
  }

  public static <R> URIO<R, R> env() {
    return access(identity());
  }

  public static <R, A, B, C> URIO<R, C> parMap2(URIO<R, ? extends A> za, URIO<R, ? extends B> zb, 
      Function2<? super A, ? super B, ? extends C> mapper) {
    return parMap2(Future.DEFAULT_EXECUTOR, za, zb, mapper);
  }

  public static <R, A, B, C> URIO<R, C> parMap2(Executor executor, URIO<R, ? extends A> za, URIO<R, ? extends B> zb, 
      Function2<? super A, ? super B, ? extends C> mapper) {
    return new URIO<>(ZIO.parMap2(executor, za.instance, zb.instance, mapper));
  }
  
  public static <R, A, B> URIO<R, Either<A, B>> race(Kind<Kind<URIO_, R>, A> fa, Kind<Kind<URIO_, R>, B> fb) {
    return race(Future.DEFAULT_EXECUTOR, fa, fb);
  }
  
  public static <R, A, B> URIO<R, Either<A, B>> race(Executor executor, Kind<Kind<URIO_, R>, A> fa, Kind<Kind<URIO_, R>, B> fb) {
    return racePair(executor, fa, fb).flatMap(either -> either.fold(
        ta -> ta.get2().cancel().fix(URIOOf.toURIO()).map(x -> Either.left(ta.get1())),
        tb -> tb.get1().cancel().fix(URIOOf.toURIO()).map(x -> Either.right(tb.get2()))));
  }
  
  public static <R, A, B> URIO<R, Either<Tuple2<A, Fiber<Kind<URIO_, R>, B>>, Tuple2<Fiber<Kind<URIO_, R>, A>, B>>> 
      racePair(Executor executor, Kind<Kind<URIO_, R>, A> fa, Kind<Kind<URIO_, R>, B> fb) {
    ZIO<R, Nothing, A> instance1 = fa.fix(URIOOf.toURIO()).instance;
    ZIO<R, Nothing, B> instance2 = fb.fix(URIOOf.toURIO()).instance;
    return new URIO<>(ZIO.racePair(executor, instance1, instance2).map(
      either -> either.bimap(a -> a.map2(f -> f.mapK(new FunctionK<Kind<Kind<ZIO_, R>, Nothing>, Kind<URIO_, R>>() {
        @Override
        public <T> URIO<R, T> apply(Kind<Kind<Kind<ZIO_, R>, Nothing>, ? extends T> from) {
          return new URIO<>(from.fix(ZIOOf::narrowK));
        }
      })), b -> b.map1(f -> f.mapK(new FunctionK<Kind<Kind<ZIO_, R>, Nothing>, Kind<URIO_, R>>() {
        @Override
        public <T> URIO<R, T> apply(Kind<Kind<Kind<ZIO_, R>, Nothing>, ? extends T> from) {
          return new URIO<>(from.fix(ZIOOf::narrowK));
        }
      })))));
  }

  public static <R, A, B> Function1<A, URIO<R, B>> lift(Function1<? super A, ? extends B> function) {
    return value -> task(() -> function.apply(value));
  }

  public static <R, A, B> Function1<A, URIO<R, B>> liftOption(Function1<? super A, Option<? extends B>> function) {
    return value -> fromOption(function.apply(value));
  }

  public static <R, A, B> Function1<A, URIO<R, B>> liftTry(Function1<? super A, Try<? extends B>> function) {
    return value -> fromTry(function.apply(value));
  }

  public static <R, A, B> Function1<A, URIO<R, B>> liftEither(Function1<? super A, Either<Throwable, ? extends B>> function) {
    return value -> fromEither(function.apply(value));
  }

  public static <R> URIO<R, Unit> sleep(Duration delay) {
    return fold(ZIO.sleep(delay));
  }

  public static <R> URIO<R, Unit> exec(CheckedRunnable task) {
    return fold(ZIO.exec(task));
  }

  public static <R, A> URIO<R, A> pure(A value) {
    return new URIO<>(ZIO.pure(value));
  }

  public static <R, A> URIO<R, A> raiseError(Throwable throwable) {
    return new URIO<>(ZIO.fromEither(() -> { throw throwable; }));
  }

  public static <R, A> URIO<R, A> defer(Producer<URIO<R, ? extends A>> lazy) {
    return new URIO<>(ZIO.defer(() -> lazy.get().instance));
  }

  public static <R, A> URIO<R, A> task(Producer<? extends A> task) {
    return fold(ZIO.task(task));
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
    return fold(ZIO.async(
        (env, cb1) -> consumer.accept(env, result -> cb1.accept(result.map(Either::right)))));
  }
  
  public static <R, A> URIO<R, A> cancellable(Function2<R, Consumer1<? super Try<? extends A>>, URIO<R, Unit>> consumer) {
    return fold(ZIO.cancellable(
        (env, cb1) -> consumer.andThen(URIO::<Throwable>toZIO).apply(env, result -> cb1.accept(result.map(Either::right)))));
  }

  public static <R, A> URIO<R, Sequence<A>> traverse(Sequence<? extends URIO<R, A>> sequence) {
    return traverse(Future.DEFAULT_EXECUTOR, sequence);
  }

  public static <R, A> URIO<R, Sequence<A>> traverse(Executor executor, Sequence<? extends URIO<R, A>> sequence) {
    return sequence.foldLeft(pure(ImmutableList.empty()), 
        (URIO<R, Sequence<A>> xs, URIO<R, A> a) -> parMap2(executor, xs, a, Sequence::append));
  }

  public static <R, A extends AutoCloseable, B> URIO<R, B> bracket(
      URIO<R, ? extends A> acquire, Function1<? super A, ? extends URIO<R, ? extends B>> use) {
    return fold(ZIO.bracket(ZIO.redeem(acquire.instance), 
        resource -> ZIO.redeem(use.andThen(URIOOf::narrowK).apply(resource).instance)));
  }

  public static <R, A, B> URIO<R, B> bracket(URIO<R, ? extends A> acquire, 
      Function1<? super A, ? extends URIO<R, ? extends B>> use, Consumer1<? super A> release) {
    return fold(ZIO.bracket(ZIO.redeem(acquire.instance), 
        resource -> ZIO.redeem(use.andThen(URIOOf::narrowK).apply(resource).instance), release));
  }

  public static <R, A, B> URIO<R, B> bracket(URIO<R, ? extends A> acquire, 
      Function1<? super A, ? extends URIO<R, ? extends B>> use, Function1<? super A, ? extends URIO<R, Unit>> release) {
    return fold(ZIO.bracket(ZIO.redeem(acquire.instance), 
        resource -> ZIO.redeem(use.andThen(URIOOf::narrowK).apply(resource).instance), release.andThen(URIO::toZIO)));
  }

  @SuppressWarnings("unchecked")
  public static <R> URIO<R, Unit> unit() {
    return (URIO<R, Unit>) UNIT;
  }

  private static <R, A> URIO<R, A> fold(ZIO<R, Throwable, A> zio) {
    return new URIO<>(zio.foldM(error -> URIO.<R, A>raiseError(error).instance, value -> URIO.<R, A>pure(value).instance));
  }
}
