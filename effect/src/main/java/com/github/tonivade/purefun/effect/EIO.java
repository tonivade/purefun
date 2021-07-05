/*
 * Copyright (c) 2018-2021, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.effect;

import static com.github.tonivade.purefun.Function2.first;
import static com.github.tonivade.purefun.Function2.second;
import static com.github.tonivade.purefun.Nothing.nothing;
import static com.github.tonivade.purefun.Precondition.checkNonNull;
import static com.github.tonivade.purefun.Producer.cons;
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
public final class EIO<E, A> implements EIOOf<E, A>, Effect<Kind<EIO_, E>, A> {

  private static final EIO<?, Unit> UNIT = new EIO<>(ZIO.unit());

  private final ZIO<Nothing, E, A> instance;

  EIO(ZIO<Nothing, E, A> value) {
    this.instance = checkNonNull(value);
  }

  @SuppressWarnings("unchecked")
  public <R> ZIO<R, E, A> toZIO() {
    return (ZIO<R, E, A>) instance;
  }
  
  public UIO<A> toUIO() {
    return new UIO<>(instance.toURIO().toZIO());
  }

  public Either<E, A> safeRunSync() {
    return instance.provide(nothing());
  }

  public Future<Either<E, A>> runAsync() {
    return instance.runAsync(nothing());
  }
  
  public void safeRunAsync(Consumer1<? super Try<? extends Either<E, ? extends A>>> callback) {
    instance.provideAsync(nothing(), callback);
  }

  @Override
  public <B> EIO<E, B> map(Function1<? super A, ? extends B> map) {
    return new EIO<>(instance.map(map));
  }

  @Override
  public <B> EIO<E, B> flatMap(Function1<? super A, ? extends Kind<Kind<EIO_, E>, ? extends B>> map) {
    return new EIO<>(instance.flatMap(value -> {
      EIO<E, ? extends B> apply = map.andThen(EIOOf::narrowK).apply(value);
      return apply.instance;
    }));
  }

  @Override
  public <B> EIO<E, B> andThen(Kind<Kind<EIO_, E>, ? extends B> next) {
    return new EIO<>(instance.andThen(next.fix(EIOOf.toEIO()).instance));
  }

  @Override
  public <B> EIO<E, B> ap(Kind<Kind<EIO_, E>, Function1<? super A, ? extends B>> apply) {
    return new EIO<>(instance.ap(apply.fix(EIOOf.toEIO()).toZIO()));
  }

  public EIO<A, E> swap() {
    return new EIO<>(instance.swap());
  }

  public <B> EIO<B, A> mapError(Function1<? super E, ? extends B> map) {
    return new EIO<>(instance.mapError(map));
  }

  public <F> EIO<F, A> flatMapError(Function1<? super E, ? extends Kind<Kind<EIO_, F>, ? extends A>> map) {
    return new EIO<>(instance.flatMapError(error -> {
      EIO<F, ? extends A> apply = map.andThen(EIOOf::narrowK).apply(error);
      return apply.instance;
    }));
  }

  public <B, F> EIO<F, B> bimap(Function1<? super E, ? extends F> mapError, Function1<? super A, ? extends B> map) {
    return new EIO<>(instance.bimap(mapError, map));
  }

  public <B, F> EIO<F, B> foldM(
      Function1<? super E, ? extends Kind<Kind<EIO_, F>, ? extends B>> mapError, 
      Function1<? super A, ? extends Kind<Kind<EIO_, F>, ? extends B>> map) {
    return new EIO<>(instance.foldM(
        error -> mapError.andThen(EIOOf::narrowK).apply(error).instance, 
        value -> map.andThen(EIOOf::narrowK).apply(value).instance));
  }

  public <B> UIO<B> fold(Function1<? super E, ? extends B> mapError, Function1<? super A, ? extends B> map) {
    return new UIO<>(instance.fold(mapError, map).<Nothing>toZIO());
  }

  public UIO<A> recover(Function1<? super E, ? extends A> mapError) {
    return new UIO<>(instance.recover(mapError).<Nothing>toZIO());
  }

  public EIO<E, A> orElse(EIO<E, ? extends A> other) {
    return new EIO<>(instance.orElse(other.instance));
  }
  
  @Override
  public <B> EIO<E, Tuple2<A, B>> zip(Kind<Kind<EIO_, E>, ? extends B> other) {
    return zipWith(other, Tuple::of);
  }
  
  @Override
  public <B> EIO<E, A> zipLeft(Kind<Kind<EIO_, E>, ? extends B> other) {
    return zipWith(other, first());
  }
  
  @Override
  public <B> EIO<E, B> zipRight(Kind<Kind<EIO_, E>, ? extends B> other) {
    return zipWith(other, second());
  }
  
  @Override
  public <B, C> EIO<E, C> zipWith(Kind<Kind<EIO_, E>, ? extends B> other, 
      Function2<? super A, ? super B, ? extends C> mapper) {
    return parMap2(this, other.fix(EIOOf.toEIO()), mapper);
  }
  
  public EIO<E, Fiber<Kind<EIO_, E>, A>> fork() {
    return new EIO<>(instance.fork().map(f -> f.mapK(new FunctionK<Kind<Kind<ZIO_, Nothing>, E>, Kind<EIO_, E>>() {
      @Override
      public <T> EIO<E, T> apply(Kind<Kind<Kind<ZIO_, Nothing>, E>, ? extends T> from) {
        return new EIO<>(from.fix(ZIOOf::narrowK));
      }
    })));
  }

  public EIO<E, A> timeout(Duration duration) {
    return timeout(Future.DEFAULT_EXECUTOR, duration);
  }
  
  public EIO<E, A> timeout(Executor executor, Duration duration) {
    return racePair(executor, this, EIO.<E>sleep(duration)).flatMap(either -> either.fold(
        ta -> ta.get2().cancel().fix(EIOOf.toEIO()).map(x -> ta.get1()),
        tb -> tb.get1().cancel().fix(EIOOf.toEIO()).flatMap(x -> EIO.throwError(new TimeoutException()))));
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
  
  public <B> EIO<E, B> repeat(Schedule<Nothing, A, B> schedule) {
    return new EIO<>(instance.repeat(schedule));
  }

  @Override
  public EIO<E, A> retry() {
    return retry(1);
  }

  @Override
  public EIO<E, A> retry(int maxRetries) {
    return retry(Schedule.recurs(maxRetries));
  }

  @Override
  public EIO<E, A> retry(Duration delay) {
    return retry(delay, 1);
  }

  @Override
  public EIO<E, A> retry(Duration delay, int maxRetries) {
    return retry(Schedule.<Nothing, E>recursSpaced(delay, maxRetries));
  }
  
  public <S> EIO<E, A> retry(Schedule<Nothing, E, S> schedule) {
    return new EIO<>(instance.retry(schedule));
  }

  @Override
  public EIO<E, Tuple2<Duration, A>> timed() {
    return new EIO<>(instance.timed());
  }
  
  public <X extends Throwable> EIO<X, A> refineOrDie(Class<X> type) {
    return new EIO<>(instance.refineOrDie(type));
  }

  public static <E, A, B, C> EIO<E, C> parMap2(EIO<E, ? extends A> za, EIO<E, ? extends B> zb, 
      Function2<? super A, ? super B, ? extends C> mapper) {
    return parMap2(Future.DEFAULT_EXECUTOR, za, zb, mapper);
  }

  public static <E, A, B, C> EIO<E, C> parMap2(Executor executor, EIO<E, ? extends A> za, EIO<E, ? extends B> zb, 
      Function2<? super A, ? super B, ? extends C> mapper) {
    return new EIO<>(ZIO.parMap2(executor, za.instance, zb.instance, mapper));
  }
  
  public static <E, A, B> EIO<E, Either<A, B>> race(Kind<Kind<EIO_, E>, A> fa, Kind<Kind<EIO_, E>, B> fb) {
    return race(Future.DEFAULT_EXECUTOR, fa, fb);
  }
  
  public static <E, A, B> EIO<E, Either<A, B>> race(Executor executor, Kind<Kind<EIO_, E>, A> fa, Kind<Kind<EIO_, E>, B> fb) {
    return racePair(executor, fa, fb).flatMap(either -> either.fold(
        ta -> ta.get2().cancel().fix(EIOOf.toEIO()).map(x -> Either.left(ta.get1())),
        tb -> tb.get1().cancel().fix(EIOOf.toEIO()).map(x -> Either.right(tb.get2()))));
  }
  
  public static <E, A, B> EIO<E, Either<Tuple2<A, Fiber<Kind<EIO_, E>, B>>, Tuple2<Fiber<Kind<EIO_, E>, A>, B>>> 
      racePair(Executor executor, Kind<Kind<EIO_, E>, A> fa, Kind<Kind<EIO_, E>, B> fb) {
    ZIO<Nothing, E, A> instance1 = fa.fix(EIOOf.toEIO()).instance;
    ZIO<Nothing, E, B> instance2 = fb.fix(EIOOf.toEIO()).instance;
    return new EIO<>(ZIO.racePair(executor, instance1, instance2).map(
      either -> either.bimap(a -> a.map2(f -> f.mapK(new FunctionK<Kind<Kind<ZIO_, Nothing>, E>, Kind<EIO_, E>>() {
        @Override
        public <T> EIO<E, T> apply(Kind<Kind<Kind<ZIO_, Nothing>, E>, ? extends T> from) {
          return new EIO<>(from.fix(ZIOOf::narrowK));
        }
      })), b -> b.map1(f -> f.mapK(new FunctionK<Kind<Kind<ZIO_, Nothing>, E>, Kind<EIO_, E>>() {
        @Override
        public <T> EIO<E, T> apply(Kind<Kind<Kind<ZIO_, Nothing>, E>, ? extends T> from) {
          return new EIO<>(from.fix(ZIOOf::narrowK));
        }
      })))));
  }

  public static <E, A> EIO<E, A> absorb(EIO<E, Either<E, A>> value) {
    return new EIO<>(ZIO.absorb(value.instance));
  }

  public static <A, B> Function1<A, EIO<Throwable, B>> lift(Function1<? super A, ? extends B> function) {
    return ZIO.<Nothing, A, B>lift(function).andThen(EIO::new);
  }

  public static <A, B> Function1<A, EIO<Throwable, B>> liftOption(Function1<? super A, Option<? extends B>> function) {
    return value -> fromOption(function.apply(value));
  }

  public static <A, B> Function1<A, EIO<Throwable, B>> liftTry(Function1<? super A, Try<? extends B>> function) {
    return value -> fromTry(function.apply(value));
  }

  public static <E, A, B> Function1<A, EIO<E, B>> liftEither(Function1<? super A, Either<E, ? extends B>> function) {
    return value -> fromEither(function.apply(value));
  }

  public static <A> EIO<Throwable, A> fromOption(Option<? extends A> task) {
    return fromOption(cons(task));
  }

  public static <A> EIO<Throwable, A> fromOption(Producer<Option<? extends A>> task) {
    return new EIO<>(ZIO.fromOption(task));
  }

  public static <A> EIO<Throwable, A> fromTry(Try<? extends A> task) {
    return fromTry(cons(task));
  }

  public static <A> EIO<Throwable, A> fromTry(Producer<Try<? extends A>> task) {
    return new EIO<>(ZIO.fromTry(task));
  }

  public static <E, A> EIO<E, A> fromEither(Either<E, ? extends A> task) {
    return fromEither(cons(task));
  }

  public static <E, A> EIO<E, A> fromEither(Producer<Either<E, ? extends A>> task) {
    return new EIO<>(ZIO.fromEither(task));
  }

  public static <E> EIO<E, Unit> sleep(Duration delay) {
    return new EIO<>(ZIO.sleep(delay));
  }

  public static EIO<Throwable, Unit> exec(CheckedRunnable task) {
    return new EIO<>(ZIO.exec(task));
  }

  public static <E, A> EIO<E, A> pure(A value) {
    return new EIO<>(ZIO.pure(value));
  }

  public static <E, A> EIO<E, A> defer(Producer<EIO<E, ? extends A>> lazy) {
    return new EIO<>(ZIO.defer(() -> lazy.get().instance));
  }

  public static <A> EIO<Throwable, A> task(Producer<? extends A> task) {
    return new EIO<>(ZIO.task(task));
  }
  
  public static <E, A> EIO<E, A> never() {
    return async(cb -> {});
  }
  
  public static <E, A> EIO<E, A> async(Consumer1<Consumer1<? super Try<? extends Either<E, ? extends A>>>> consumer) {
    return new EIO<>(ZIO.async((env, cb) -> consumer.accept(cb)));
  }
  
  public static <E, A> EIO<E, A> cancellable(Function1<Consumer1<? super Try<? extends Either<E, ? extends A>>>, EIO<E, Unit>> consumer) {
    return new EIO<>(ZIO.cancellable((env, cb) -> consumer.andThen(EIO::<Nothing>toZIO).apply(cb)));
  }

  public static <E, A> EIO<E, A> raiseError(E error) {
    return new EIO<>(ZIO.raiseError(error));
  }

  public static <E, A> EIO<E, A> throwError(Throwable error) {
    return new EIO<>(ZIO.throwError(error));
  }

  public static <E, A> EIO<E, Sequence<A>> traverse(Sequence<? extends EIO<E, A>> sequence) {
    return traverse(Future.DEFAULT_EXECUTOR, sequence);
  }

  public static <E, A> EIO<E, Sequence<A>> traverse(Executor executor, Sequence<? extends EIO<E, A>> sequence) {
    return sequence.foldLeft(pure(ImmutableList.empty()), 
        (EIO<E, Sequence<A>> xs, EIO<E, A> a) -> parMap2(executor, xs, a, Sequence::append));
  }

  public static <E, A extends AutoCloseable, B> EIO<E, B> bracket(EIO<E, ? extends A> acquire, 
      Function1<? super A, ? extends EIO<E, ? extends B>> use) {
    return new EIO<>(ZIO.bracket(acquire.instance, 
        resource -> use.andThen(EIOOf::<E, B>narrowK).apply(resource).instance));
  }

  public static <E, A, B> EIO<E, B> bracket(EIO<E, ? extends A> acquire, 
      Function1<? super A, ? extends EIO<E, ? extends B>> use, Consumer1<? super A> release) {
    return new EIO<>(ZIO.bracket(acquire.instance, 
        resource -> use.andThen(EIOOf::<E, B>narrowK).apply(resource).instance, release));
  }

  public static <E, A, B> EIO<E, B> bracket(EIO<E, ? extends A> acquire, 
      Function1<? super A, ? extends EIO<E, ? extends B>> use, Function1<? super A, ? extends EIO<E, Unit>> release) {
    return new EIO<>(ZIO.bracket(acquire.instance, 
        resource -> use.andThen(EIOOf::<E, B>narrowK).apply(resource).instance, release.andThen(EIO::toZIO)));
  }

  @SuppressWarnings("unchecked")
  public static <E> EIO<E, Unit> unit() {
    return (EIO<E, Unit>) UNIT;
  }
}
