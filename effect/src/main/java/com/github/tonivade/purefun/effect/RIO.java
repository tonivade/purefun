/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.effect;

import static com.github.tonivade.purefun.Function1.identity;
import static com.github.tonivade.purefun.Precondition.checkNonNull;
import java.time.Duration;
import java.util.concurrent.Executor;
import com.github.tonivade.purefun.CheckedRunnable;
import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Function2;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Witness;
import com.github.tonivade.purefun.Nothing;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Recoverable;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.concurrent.Future;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Try;
import com.github.tonivade.purefun.typeclasses.MonadDefer;

@HigherKind
public final class RIO<R, T> implements RIOOf<R, T>, Recoverable {

  private static final RIO<?, Unit> UNIT = new RIO<>(ZIO.unit());

  private final ZIO<R, Throwable, T> instance;

  RIO(ZIO<R, Throwable, T> value) {
    this.instance = checkNonNull(value);
  }

  public Try<T> provide(R env) {
    return Try.fromEither(instance.provide(env));
  }

  public ZIO<R, Throwable, T> toZIO() {
    return instance;
  }

  @SuppressWarnings("unchecked")
  public <E> EIO<E, T> toEIO() {
    return new EIO<>((ZIO<Nothing, E, T>) instance);
  }

  public Future<Try<T>> toFuture(R env) {
    return instance.toFuture(env).map(Try::fromEither);
  }

  public void async(Executor executor, R env, Consumer1<Try<T>> callback) {
    instance.provideAsync(executor, env, result -> callback.accept(flatAbsorb(result)));
  }

  public void async(R env, Consumer1<Try<T>> callback) {
    async(Future.DEFAULT_EXECUTOR, env, callback);
  }

  public <F extends Witness> Kind<F, T> foldMap(R env, MonadDefer<F> monad) {
    return monad.flatMap(instance.foldMap(env, monad), monad::<T>fromEither);
  }

  public <B> RIO<R, B> map(Function1<T, B> map) {
    return new RIO<>(instance.map(map));
  }

  public <B> RIO<R, B> flatMap(Function1<T, RIO<R, B>> map) {
    return new RIO<>(instance.flatMap(x -> map.apply(x).instance));
  }

  public <B> RIO<R, B> andThen(RIO<R, B> next) {
    return new RIO<>(instance.andThen(next.instance));
  }

  public RIO<R, T> recover(Function1<Throwable, T> mapError) {
    return fold(mapError, identity());
  }

  @SuppressWarnings("unchecked")
  public <X extends Throwable> RIO<R, T> recoverWith(Class<X> type, Function1<X, T> function) {
    return recover(cause -> {
      if (type.isAssignableFrom(cause.getClass())) {
        return function.apply((X) cause);
      }
      return sneakyThrow(cause);
    });
  }

  public <B> RIO<R, B> fold(Function1<Throwable, B> mapError, Function1<T, B> map) {
    return foldM(mapError.andThen(RIO::pure), map.andThen(RIO::pure));
  }

  public <B> RIO<R, B> foldM(Function1<Throwable, RIO<R, B>> mapError, Function1<T, RIO<R, B>> map) {
    return new RIO<>(instance.foldM(error -> mapError.apply(error).instance, x -> map.apply(x).instance));
  }

  public RIO<R, T> repeat() {
    return repeat(1);
  }

  public RIO<R, T> repeat(int times) {
    return repeat(unit(), times);
  }

  public RIO<R, T> repeat(Duration delay) {
    return repeat(delay, 1);
  }

  public RIO<R, T> repeat(Duration delay, int times) {
    return repeat(sleep(delay), times);
  }

  public RIO<R, T> retry() {
    return retry(1);
  }

  public RIO<R, T> retry(int maxRetries) {
    return retry(unit(), maxRetries);
  }

  public RIO<R, T> retry(Duration delay) {
    return retry(delay, 1);
  }

  public RIO<R, T> retry(Duration delay, int maxRetries) {
    return retry(sleep(delay), maxRetries);
  }

  private RIO<R, T> repeat(RIO<R, Unit> pause, int times) {
    return foldM(RIO::<R, T>raiseError, value -> {
      if (times > 0) {
        return pause.andThen(repeat(pause, times - 1));
      } else {
        return pure(value);
      }
    });
  }

  private RIO<R, T> retry(RIO<R, Unit> pause, int maxRetries) {
    return foldM(error -> {
      if (maxRetries > 0) {
        return pause.andThen(retry(pause.repeat(), maxRetries - 1));
      } else {
        return raiseError(error);
      }
    }, RIO::<R, T>pure);
  }

  public static <R, A> RIO<R, A> access(Function1<R, A> map) {
    return new RIO<>(ZIO.accessM(map.andThen(ZIO::pure)));
  }

  public static <R> RIO<R, R> env() {
    return access(identity());
  }

  public static <R, A, B, C> RIO<R, C> map2(RIO<R, A> za, RIO<R, B> zb, Function2<A, B, C> mapper) {
    return new RIO<>(ZIO.map2(za.instance, zb.instance, mapper));
  }

  public static <R> RIO<R, Unit> sleep(Duration delay) {
    return new RIO<>(ZIO.sleep(delay));
  }

  public static <R> RIO<R, Unit> exec(CheckedRunnable task) {
    return new RIO<>(ZIO.exec(task));
  }

  public static <R, A> RIO<R, A> pure(A value) {
    return new RIO<>(ZIO.pure(value));
  }

  public static <R, A> RIO<R, A> raiseError(Throwable throwable) {
    return new RIO<>(ZIO.raiseError(throwable));
  }

  public static <R, A> RIO<R, A> defer(Producer<RIO<R, A>> lazy) {
    return new RIO<>(ZIO.defer(() -> lazy.get().instance));
  }

  public static <R, A> RIO<R, A> task(Producer<A> task) {
    return new RIO<>(ZIO.task(task));
  }

  public static <R, A extends AutoCloseable, B> RIO<R, B> bracket(RIO<R, A> acquire, Function1<A, RIO<R, B>> use) {
    return new RIO<>(ZIO.bracket(acquire.instance, resource -> use.apply(resource).instance));
  }

  public static <R, A, B> RIO<R, B> bracket(RIO<R, A> acquire, Function1<A, RIO<R, B>> use, Consumer1<A> release) {
    return new RIO<>(ZIO.bracket(acquire.instance, resource -> use.apply(resource).instance, release));
  }

  @SuppressWarnings("unchecked")
  public static <R> RIO<R, Unit> unit() {
    return (RIO<R, Unit>) UNIT;
  }

  private Try<T> flatAbsorb(Try<Either<Throwable, T>> result) {
    return result.map(Try::fromEither).flatMap(identity());
  }
}
