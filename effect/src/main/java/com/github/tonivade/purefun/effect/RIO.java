/*
 * Copyright (c) 2018-2021, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.effect;

import static com.github.tonivade.purefun.Function1.identity;
import static com.github.tonivade.purefun.Function2.first;
import static com.github.tonivade.purefun.Function2.second;
import static com.github.tonivade.purefun.Precondition.checkNonNull;
import static com.github.tonivade.purefun.Producer.cons;
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
public final class RIO<R, A> implements RIOOf<R, A>, Effect<Kind<RIO_, R>, A>, Recoverable {

  private static final RIO<?, Unit> UNIT = new RIO<>(PureIO.unit());

  private final PureIO<R, Throwable, A> instance;

  RIO(PureIO<R, Throwable, A> value) {
    this.instance = checkNonNull(value);
  }

  public Try<A> safeRunSync(R env) {
    return Try.fromEither(instance.provide(env));
  }

  public PureIO<R, Throwable, A> toPureIO() {
    return instance;
  }

  @SuppressWarnings("unchecked")
  public <E> EIO<E, A> toEIO() {
    return new EIO<>((PureIO<Nothing, E, A>) instance);
  }
  
  public URIO<R, A> toURIO() {
    return recover(this::sneakyThrow);
  }

  public Future<A> runAsync(R env) {
    return instance.runAsync(env).flatMap(e -> e.fold(Future::failure, Future::success));
  }

  public Future<A> runAsync(R env, Executor executor) {
    return RIO.<R>forked(executor).andThen(this).runAsync(env);
  }

  public void safeRunAsync(R env, Consumer1<? super Try<? extends A>> callback) {
    instance.provideAsync(env, result -> callback.accept(result.flatMap(Try::fromEither)));
  }

  @Override
  public <B> RIO<R, B> map(Function1<? super A, ? extends B> map) {
    return new RIO<>(instance.map(map));
  }

  @Override
  public <B> RIO<R, B> flatMap(Function1<? super A, ? extends Kind<Kind<RIO_, R>, ? extends B>> map) {
    return new RIO<>(instance.flatMap(x -> {
      RIO<R, ? extends B> apply = map.andThen(RIOOf::narrowK).apply(x);
      return apply.instance;
    }));
  }

  @Override
  public <B> RIO<R, B> andThen(Kind<Kind<RIO_, R>, ? extends B> next) {
    return new RIO<>(instance.andThen(next.fix(RIOOf.toRIO()).instance));
  }

  @Override
  public <B> RIO<R, B> ap(Kind<Kind<RIO_, R>, Function1<? super A, ? extends B>> apply) {
    return new RIO<>(instance.ap(apply.fix(RIOOf.toRIO()).instance));
  }

  public URIO<R, A> recover(Function1<? super Throwable, ? extends A> mapError) {
    return fold(mapError, identity());
  }

  @SuppressWarnings("unchecked")
  public <X extends Throwable> URIO<R, A> recoverWith(Class<X> type, Function1<? super X, ? extends A> function) {
    return recover(cause -> {
      if (type.isAssignableFrom(cause.getClass())) {
        return function.apply((X) cause);
      }
      return sneakyThrow(cause);
    });
  }

  public <B> URIO<R, B> fold(
      Function1<? super Throwable, ? extends B> mapError, Function1<? super A, ? extends B> map) {
    return new URIO<>(instance.foldM(mapError.andThen(PureIO::pure), map.andThen(PureIO::pure)));
  }

  public <B> RIO<R, B> foldM(
      Function1<? super Throwable, ? extends Kind<Kind<RIO_, R>, ? extends B>> mapError, 
      Function1<? super A, ? extends Kind<Kind<RIO_, R>, ? extends B>> map) {
    return new RIO<>(instance.foldM(
        error -> mapError.andThen(RIOOf::narrowK).apply(error).instance, 
        value -> map.andThen(RIOOf::narrowK).apply(value).instance));
  }

  public RIO<R, A> orElse(Kind<Kind<RIO_, R>, ? extends A> other) {
    return foldM(Function1.cons(other), Function1.cons(this));
  }
  
  @Override
  public <B> RIO<R, Tuple2<A, B>> zip(Kind<Kind<RIO_, R>, ? extends B> other) {
    return zipWith(other, Tuple::of);
  }
  
  @Override
  public <B> RIO<R, A> zipLeft(Kind<Kind<RIO_, R>, ? extends B> other) {
    return zipWith(other, first());
  }
  
  @Override
  public <B> RIO<R, B> zipRight(Kind<Kind<RIO_, R>, ? extends B> other) {
    return zipWith(other, second());
  }
  
  @Override
  public <B, C> RIO<R, C> zipWith(Kind<Kind<RIO_, R>, ? extends B> other, 
      Function2<? super A, ? super B, ? extends C> mapper) {
    return parMap2(this, other.fix(RIOOf.toRIO()), mapper);
  }
  
  public RIO<R, Fiber<Kind<RIO_, R>, A>> fork() {
    return new RIO<>(instance.fork().map(f -> f.mapK(new FunctionK<Kind<Kind<PureIO_, R>, Throwable>, Kind<RIO_, R>>() {
      @Override
      public <T> RIO<R, T> apply(Kind<Kind<Kind<PureIO_, R>, Throwable>, ? extends T> from) {
        return new RIO<>(from.fix(PureIOOf::narrowK));
      }
    })));
  }

  public RIO<R, A> timeout(Duration duration) {
    return timeout(Future.DEFAULT_EXECUTOR, duration);
  }
  
  public RIO<R, A> timeout(Executor executor, Duration duration) {
    return racePair(executor, this, sleep(duration)).flatMap(either -> either.fold(
        ta -> ta.get2().cancel().fix(RIOOf.toRIO()).map(x -> ta.get1()),
        tb -> tb.get1().cancel().fix(RIOOf.toRIO()).flatMap(x -> RIO.raiseError(new TimeoutException()))));
  }

  @Override
  public RIO<R, A> repeat() {
    return repeat(1);
  }

  @Override
  public RIO<R, A> repeat(int times) {
    return new RIO<>(instance.repeat(times));
  }

  @Override
  public RIO<R, A> repeat(Duration delay) {
    return repeat(delay, 1);
  }

  @Override
  public RIO<R, A> repeat(Duration delay, int times) {
    return new RIO<>(instance.repeat(delay, times));
  }
  
  public <B> RIO<R, B> repeat(Schedule<R, A, B> schedule) {
    return new RIO<>(instance.repeat(schedule));
  }

  @Override
  public RIO<R, A> retry() {
    return retry(1);
  }

  @Override
  public RIO<R, A> retry(int maxRetries) {
    return retry(Schedule.recurs(maxRetries));
  }

  @Override
  public RIO<R, A> retry(Duration delay) {
    return retry(delay, 1);
  }

  @Override
  public RIO<R, A> retry(Duration delay, int maxRetries) {
    return retry(Schedule.<R, Throwable>recursSpaced(delay, maxRetries));
  }
  
  public <B> RIO<R, A> retry(Schedule<R, Throwable, B> schedule) {
    return new RIO<>(instance.retry(schedule));
  }

  @Override
  public RIO<R, Tuple2<Duration, A>> timed() {
    return new RIO<>(instance.timed());
  }
  
  public static <R> RIO<R, Unit> forked(Executor executor) {
    return async((env, callback) -> executor.execute(() -> callback.accept(Try.success(Unit.unit()))));
  }

  public static <R, A> RIO<R, A> accessM(Function1<? super R, ? extends RIO<R, ? extends A>> map) {
    return new RIO<>(PureIO.accessM(map.andThen(RIO::toPureIO)));
  }

  public static <R, A> RIO<R, A> access(Function1<? super R, ? extends A> map) {
    return accessM(map.andThen(RIO::pure));
  }

  public static <R> RIO<R, R> env() {
    return access(identity());
  }

  public static <R, A> RIO<R, A> absorb(RIO<R, Either<Throwable, A>> value) {
    return new RIO<>(PureIO.absorb(value.instance));
  }

  public static <R, A, B, C> RIO<R, C> parMap2(Kind<Kind<RIO_, R>, ? extends A> za, Kind<Kind<RIO_, R>, ? extends B> zb, 
      Function2<? super A, ? super B, ? extends C> mapper) {
    return parMap2(Future.DEFAULT_EXECUTOR, za, zb, mapper);
  }

  public static <R, A, B, C> RIO<R, C> parMap2(Executor executor, Kind<Kind<RIO_, R>, ? extends A> za, Kind<Kind<RIO_, R>, ? extends B> zb, 
      Function2<? super A, ? super B, ? extends C> mapper) {
    return new RIO<>(PureIO.parMap2(executor, za.fix(RIOOf::narrowK).instance, zb.fix(RIOOf::narrowK).instance, mapper));
  }
  
  public static <R, A, B> RIO<R, Either<A, B>> race(Kind<Kind<RIO_, R>, ? extends A> fa, Kind<Kind<RIO_, R>, ? extends B> fb) {
    return race(Future.DEFAULT_EXECUTOR, fa, fb);
  }
  
  public static <R, A, B> RIO<R, Either<A, B>> race(Executor executor, Kind<Kind<RIO_, R>, ? extends A> fa, Kind<Kind<RIO_, R>, ? extends B> fb) {
    return racePair(executor, fa, fb).flatMap(either -> either.fold(
        ta -> ta.get2().cancel().fix(RIOOf.toRIO()).map(x -> Either.left(ta.get1())),
        tb -> tb.get1().cancel().fix(RIOOf.toRIO()).map(x -> Either.right(tb.get2()))));
  }
  
  public static <R, A, B> RIO<R, Either<Tuple2<A, Fiber<Kind<RIO_, R>, B>>, Tuple2<Fiber<Kind<RIO_, R>, A>, B>>> 
      racePair(Executor executor, Kind<Kind<RIO_, R>, ? extends A> fa, Kind<Kind<RIO_, R>, ? extends B> fb) {
    PureIO<R, Throwable, A> instance1 = fa.fix(RIOOf.toRIO()).instance.fix(PureIOOf::narrowK);
    PureIO<R, Throwable, B> instance2 = fb.fix(RIOOf.toRIO()).instance.fix(PureIOOf::narrowK);
    return new RIO<>(PureIO.racePair(executor, instance1, instance2).map(
      either -> either.bimap(a -> a.map2(f -> f.mapK(new FunctionK<Kind<Kind<PureIO_, R>, Throwable>, Kind<RIO_, R>>() {
        @Override
        public <T> RIO<R, T> apply(Kind<Kind<Kind<PureIO_, R>, Throwable>, ? extends T> from) {
          return new RIO<>(from.fix(PureIOOf::narrowK));
        }
      })), b -> b.map1(f -> f.mapK(new FunctionK<Kind<Kind<PureIO_, R>, Throwable>, Kind<RIO_, R>>() {
        @Override
        public <T> RIO<R, T> apply(Kind<Kind<Kind<PureIO_, R>, Throwable>, ? extends T> from) {
          return new RIO<>(from.fix(PureIOOf::narrowK));
        }
      })))));
  }

  public static <R> RIO<R, Unit> sleep(Duration delay) {
    return new RIO<>(PureIO.sleep(delay));
  }

  public static <R, A, B> Function1<A, RIO<R, B>> lift(Function1<? super A, ? extends B> function) {
    return value -> task(() -> function.apply(value));
  }

  public static <R, A, B> Function1<A, RIO<R, B>> liftOption(Function1<? super A, Option<? extends B>> function) {
    return value -> fromOption(function.apply(value));
  }

  public static <R, A, B> Function1<A, RIO<R, B>> liftTry(Function1<? super A, Try<? extends B>> function) {
    return value -> fromTry(function.apply(value));
  }

  public static <R, A, B> Function1<A, RIO<R, B>> liftEither(Function1<? super A, Either<Throwable, ? extends B>> function) {
    return value -> fromEither(function.apply(value));
  }

  public static <R, A> RIO<R, A> fromOption(Option<? extends A> task) {
    return fromOption(cons(task));
  }

  public static <R, A> RIO<R, A> fromOption(Producer<Option<? extends A>> task) {
    return fromEither(task.andThen(Option::toEither));
  }

  public static <R, A> RIO<R, A> fromTry(Try<? extends A> task) {
    return fromTry(cons(task));
  }

  public static <R, A> RIO<R, A> fromTry(Producer<Try<? extends A>> task) {
    return new RIO<>(PureIO.fromTry(task));
  }

  public static <R, A> RIO<R, A> fromEither(Either<Throwable, ? extends A> task) {
    return fromEither(cons(task));
  }

  public static <R, A> RIO<R, A> fromEither(Producer<Either<Throwable, ? extends A>> task) {
    return new RIO<>(PureIO.fromEither(task));
  }

  public static <R> RIO<R, Unit> exec(CheckedRunnable task) {
    return new RIO<>(PureIO.exec(task));
  }

  public static <R, A> RIO<R, A> pure(A value) {
    return new RIO<>(PureIO.pure(value));
  }

  public static <R, A> RIO<R, A> raiseError(Throwable throwable) {
    return new RIO<>(PureIO.raiseError(throwable));
  }

  public static <R, A> RIO<R, A> defer(Producer<Kind<Kind<RIO_, R>, ? extends A>> lazy) {
    return new RIO<>(PureIO.defer(() -> lazy.andThen(RIOOf::narrowK).get().instance));
  }

  public static <R, A> RIO<R, A> task(Producer<? extends A> task) {
    return new RIO<>(PureIO.task(task));
  }
  
  public static <R, A> RIO<R, A> never() {
    return async((env, cb) -> {});
  }
  
  public static <R, A> RIO<R, A> async(Consumer2<R, Consumer1<? super Try<? extends A>>> consumer) {
    return new RIO<>(PureIO.async(
      (env, cb1) -> consumer.accept(env, result -> cb1.accept(result.map(Either::right)))));
  }
  
  public static <R, A> RIO<R, A> cancellable(Function2<R, Consumer1<? super Try<? extends A>>, RIO<R, Unit>> consumer) {
    return new RIO<>(PureIO.cancellable(
      (env, cb1) -> consumer.andThen(RIO::toPureIO).apply(env, result -> cb1.accept(result.map(Either::right)))));
  }

  public static <R, A> RIO<R, Sequence<A>> traverse(Sequence<? extends RIO<R, A>> sequence) {
    return traverse(Future.DEFAULT_EXECUTOR, sequence);
  }

  public static <R, A> RIO<R, Sequence<A>> traverse(Executor executor, Sequence<? extends RIO<R, A>> sequence) {
    return sequence.foldLeft(pure(ImmutableList.empty()), 
        (RIO<R, Sequence<A>> xs, RIO<R, A> a) -> parMap2(executor, xs, a, Sequence::append));
  }

  public static <R, A extends AutoCloseable, B> RIO<R, B> bracket(Kind<Kind<RIO_, R>, ? extends A> acquire, 
      Function1<? super A, ? extends Kind<Kind<RIO_, R>, ? extends B>> use) {
    return new RIO<>(PureIO.bracket(acquire.fix(RIOOf::narrowK).instance, 
        resource -> use.andThen(RIOOf::narrowK).apply(resource).instance));
  }

  public static <R, A, B> RIO<R, B> bracket(Kind<Kind<RIO_, R>, ? extends A> acquire, 
      Function1<? super A, ? extends Kind<Kind<RIO_, R>, ? extends B>> use, Consumer1<? super A> release) {
    return new RIO<>(PureIO.bracket(acquire.fix(RIOOf::narrowK).instance, 
        resource -> use.andThen(RIOOf::narrowK).apply(resource).instance, release));
  }

  public static <R, A, B> RIO<R, B> bracket(Kind<Kind<RIO_, R>, ? extends A> acquire, 
      Function1<? super A, ? extends Kind<Kind<RIO_, R>, ? extends B>> use, Function1<? super A, ? extends Kind<Kind<RIO_, R>, Unit>> release) {
    return new RIO<>(PureIO.bracket(acquire.fix(RIOOf::narrowK).instance, 
        resource -> use.andThen(RIOOf::narrowK).apply(resource).instance, release.andThen(RIOOf::narrowK).andThen(RIO::toPureIO)));
  }

  @SuppressWarnings("unchecked")
  public static <R> RIO<R, Unit> unit() {
    return (RIO<R, Unit>) UNIT;
  }
}
