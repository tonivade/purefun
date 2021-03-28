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

import com.github.tonivade.purefun.CheckedRunnable;
import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Function2;
import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Nothing;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Tuple;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.Witness;
import com.github.tonivade.purefun.concurrent.Future;
import com.github.tonivade.purefun.data.ImmutableList;
import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.type.Try;
import com.github.tonivade.purefun.typeclasses.Async;

@HigherKind
public final class EIO<E, A> implements EIOOf<E, A> {

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

  public Future<A> toFuture() {
    return toFuture(Future.DEFAULT_EXECUTOR);
  }

  public Future<A> toFuture(Executor executor) {
    return instance.toFuture(executor, nothing());
  }

  public void safeRunAsync(Executor executor, Consumer1<? super Try<? extends A>> callback) {
    instance.provideAsync(executor, nothing(), callback);
  }

  public void safeRunAsync(Consumer1<? super Try<? extends A>> callback) {
    safeRunAsync(Future.DEFAULT_EXECUTOR, callback);
  }

  public <F extends Witness> Kind<F, A> foldMap(Async<F> monad) {
    return instance.foldMap(nothing(), monad);
  }

  public <B> EIO<E, B> map(Function1<? super A, ? extends B> map) {
    return new EIO<>(instance.map(map));
  }

  public <B> EIO<E, B> flatMap(Function1<? super A, ? extends EIO<E, ? extends B>> map) {
    return new EIO<>(instance.flatMap(value -> {
      EIO<E, ? extends B> apply = map.apply(value);
      return apply.instance;
    }));
  }

  public EIO<A, E> swap() {
    return new EIO<>(instance.swap());
  }

  public <B> EIO<B, A> mapError(Function1<? super E, ? extends B> map) {
    return new EIO<>(instance.mapError(map));
  }

  public <F> EIO<F, A> flatMapError(Function1<? super E, ? extends EIO<F, ? extends A>> map) {
    return new EIO<>(instance.flatMapError(error -> {
      EIO<F, ? extends A> apply = map.apply(error);
      return apply.instance;
    }));
  }

  public <B, F> EIO<F, B> bimap(Function1<? super E, ? extends F> mapError, Function1<? super A, ? extends B> map) {
    return new EIO<>(instance.bimap(mapError, map));
  }

  public <B> EIO<E, B> andThen(EIO<E, ? extends B> next) {
    return new EIO<>(instance.andThen(next.instance));
  }

  public <B> EIO<E, B> ap(EIO<E, Function1<? super A, ? extends B>> apply) {
    return new EIO<>(instance.ap(apply.toZIO()));
  }

  public <B, F> EIO<F, B> foldM(
      Function1<? super E, ? extends EIO<F, ? extends B>> mapError, 
      Function1<? super A, ? extends EIO<F, ? extends B>> map) {
    return new EIO<>(instance.foldM(
        error -> mapError.andThen(EIOOf::narrowK).apply(error).instance, 
        value -> map.andThen(EIOOf::narrowK).apply(value).instance));
  }

  public <B> UIO<B> fold(Function1<? super E, ? extends B> mapError, Function1<? super A, ? extends B> map) {
    return new UIO<>(instance.fold(mapError, map));
  }

  public UIO<A> recover(Function1<? super E, ? extends A> mapError) {
    return new UIO<>(instance.recover(mapError));
  }

  public EIO<E, A> orElse(EIO<E, ? extends A> other) {
    return new EIO<>(instance.orElse(other.instance));
  }
  
  public <B> EIO<E, Tuple2<A, B>> zip(EIO<E, ? extends B> other) {
    return zipWith(other, Tuple::of);
  }
  
  public <B> EIO<E, A> zipLeft(EIO<E, ? extends B> other) {
    return zipWith(other, first());
  }
  
  public <B> EIO<E, B> zipRight(EIO<E, ? extends B> other) {
    return zipWith(other, second());
  }
  
  public <B, C> EIO<E, C> zipWith(EIO<E, ? extends B> other, 
      Function2<? super A, ? super B, ? extends C> mapper) {
    return map2(this, other, mapper);
  }

  public EIO<E, A> repeat() {
    return repeat(1);
  }

  public EIO<E, A> repeat(int times) {
    return new EIO<>(instance.repeat(times));
  }

  public EIO<E, A> repeat(Duration delay) {
    return repeat(delay, 1);
  }

  public EIO<E, A> repeat(Duration delay, int times) {
    return new EIO<>(instance.repeat(delay, times));
  }
  
  public <B> EIO<E, B> repeat(Schedule<Nothing, A, B> schedule) {
    return new EIO<>(instance.repeat(schedule));
  }

  public EIO<E, A> retry() {
    return retry(1);
  }

  public EIO<E, A> retry(int maxRetries) {
    return retry(Schedule.recurs(maxRetries));
  }

  public EIO<E, A> retry(Duration delay) {
    return retry(delay, 1);
  }

  public EIO<E, A> retry(Duration delay, int maxRetries) {
    return retry(Schedule.<Nothing, E>recursSpaced(delay, maxRetries));
  }
  
  public <S> EIO<E, A> retry(Schedule<Nothing, E, S> schedule) {
    return new EIO<>(instance.retry(schedule));
  }

  public EIO<E, Tuple2<Duration, A>> timed() {
    return new EIO<>(instance.timed());
  }
  
  public <X extends Throwable> EIO<X, A> refineOrDie(Class<X> type) {
    return new EIO<>(instance.refineOrDie(type));
  }

  public static <E, A, B, C> EIO<E, C> map2(EIO<E, ? extends A> za, EIO<E, ? extends B> zb, 
      Function2<? super A, ? super B, ? extends C> mapper) {
    return new EIO<>(ZIO.map2(za.instance, zb.instance, mapper));
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
  
  public static <A> EIO<Throwable, A> async(Consumer1<Consumer1<? super Try<? extends A>>> consumer) {
    return new EIO<>(ZIO.async(consumer));
  }
  
  public static <A> EIO<Throwable, A> asyncF(Function1<Consumer1<? super Try<? extends A>>, UIO<Unit>> consumer) {
    return new EIO<>(ZIO.asyncF(consumer));
  }

  public static <E, A> EIO<E, A> raiseError(E error) {
    return new EIO<>(ZIO.raiseError(error));
  }

  public static <E, A> EIO<E, Sequence<A>> traverse(Sequence<? extends EIO<E, A>> sequence) {
    return sequence.foldLeft(pure(ImmutableList.empty()), 
        (EIO<E, Sequence<A>> xs, EIO<E, A> a) -> map2(xs, a, Sequence::append));
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

  @SuppressWarnings("unchecked")
  public static <E> EIO<E, Unit> unit() {
    return (EIO<E, Unit>) UNIT;
  }
}
