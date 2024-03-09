/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.effect;

import static com.github.tonivade.purefun.core.Function1.identity;
import static com.github.tonivade.purefun.core.Function2.first;
import static com.github.tonivade.purefun.core.Function2.second;
import static com.github.tonivade.purefun.core.Matcher1.always;
import static com.github.tonivade.purefun.core.Precondition.checkNonNull;
import static com.github.tonivade.purefun.core.Producer.cons;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nullable;

import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.concurrent.Future;
import com.github.tonivade.purefun.concurrent.Promise;
import com.github.tonivade.purefun.core.CheckedRunnable;
import com.github.tonivade.purefun.core.Consumer1;
import com.github.tonivade.purefun.core.Consumer2;
import com.github.tonivade.purefun.core.Effect;
import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Function2;
import com.github.tonivade.purefun.core.Nothing;
import com.github.tonivade.purefun.core.Operator1;
import com.github.tonivade.purefun.core.PartialFunction1;
import com.github.tonivade.purefun.core.Producer;
import com.github.tonivade.purefun.core.Recoverable;
import com.github.tonivade.purefun.core.Tuple;
import com.github.tonivade.purefun.core.Tuple2;
import com.github.tonivade.purefun.core.Unit;
import com.github.tonivade.purefun.data.ImmutableList;
import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.EitherOf;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.type.Try;
import com.github.tonivade.purefun.typeclasses.Fiber;

@HigherKind
public sealed interface PureIO<R, E, A> extends PureIOOf<R, E, A>, Effect<Kind<Kind<PureIO_, R>, E>, A> {

  default Either<E, A> provide(@Nullable R env) {
    return runAsync(env).getOrElseThrow();
  }

  default Future<Either<E, A>> runAsync(@Nullable R env) {
    return Future.from(runAsync(env, this, PureIOConnection.UNCANCELLABLE));
  }

  default Future<Either<E, A>> runAsync(@Nullable R env, Executor executor) {
    return PureIO.<R, E>forked(executor).andThen(this).runAsync(env);
  }

  default void provideAsync(@Nullable R env, Consumer1<? super Try<? extends Either<E, A>>> callback) {
    runAsync(env).onComplete(callback::accept);
  }

  default void provideAsync(@Nullable R env, Executor executor, Consumer1<? super Try<? extends Either<E, A>>> callback) {
    runAsync(env, executor).onComplete(callback::accept);
  }

  default PureIO<R, A, E> swap() {
    return foldM(PureIO::pure, PureIO::raiseError);
  }

  @Override
  default <B> PureIO<R, E, B> map(Function1<? super A, ? extends B> map) {
    return flatMap(map.andThen(PureIO::pure));
  }

  default <B> PureIO<R, B, A> mapError(Function1<? super E, ? extends B> map) {
    return flatMapError(map.andThen(PureIO::raiseError));
  }

  default <B, F> PureIO<R, F, B> bimap(Function1<? super E, ? extends F> mapError, Function1<? super A, ? extends B> map) {
    return foldM(mapError.andThen(PureIO::raiseError), map.andThen(PureIO::pure));
  }

  @Override
  default <B> PureIO<R, E, B> flatMap(Function1<? super A, ? extends Kind<Kind<Kind<PureIO_, R>, E>, ? extends B>> map) {
    return foldM(PureIO::raiseError, map);
  }

  default <F> PureIO<R, F, A> flatMapError(Function1<? super E, ? extends Kind<Kind<Kind<PureIO_, R>, F>, ? extends A>> map) {
    return foldM(map, PureIO::pure);
  }

  default <F, B> PureIO<R, F, B> foldM(
      Function1<? super E, ? extends Kind<Kind<Kind<PureIO_, R>, F>, ? extends B>> left,
      Function1<? super A, ? extends Kind<Kind<Kind<PureIO_, R>, F>, ? extends B>> right) {
    return new FlatMapped<>(this, left, right);
  }

  @Override
  default <B> PureIO<R, E, B> andThen(Kind<Kind<Kind<PureIO_, R>, E>, ? extends B> next) {
    return flatMap(ignore -> next);
  }

  @Override
  default <B> PureIO<R, E, B> ap(Kind<Kind<Kind<PureIO_, R>, E>, ? extends Function1<? super A, ? extends B>> apply) {
    return parMap2(this, apply.fix(PureIOOf.toPureIO()), (v, a) -> a.apply(v));
  }

  default <B> URIO<R, B> fold(
      Function1<? super E, ? extends B> mapError, Function1<? super A, ? extends B> map) {
    return new URIO<>(foldM(mapError.andThen(PureIO::pure), map.andThen(PureIO::pure)));
  }

  default URIO<R, A> recover(Function1<? super E, ? extends A> mapError) {
    return fold(mapError, identity());
  }

  default PureIO<R, E, A> orElse(Kind<Kind<Kind<PureIO_, R>, E>, ? extends A> other) {
    return foldM(Function1.cons(other), Function1.cons(this));
  }

  @Override
  default <B> PureIO<R, E, Tuple2<A, B>> zip(Kind<Kind<Kind<PureIO_, R>, E>, ? extends B> other) {
    return zipWith(other, Tuple::of);
  }

  @Override
  default <B> PureIO<R, E, A> zipLeft(Kind<Kind<Kind<PureIO_, R>, E>, ? extends B> other) {
    return zipWith(other, first());
  }

  @Override
  default <B> PureIO<R, E, B> zipRight(Kind<Kind<Kind<PureIO_, R>, E>, ? extends B> other) {
    return zipWith(other, second());
  }

  @Override
  default <B, C> PureIO<R, E, C> zipWith(Kind<Kind<Kind<PureIO_, R>, E>, ? extends B> other,
      Function2<? super A, ? super B, ? extends C> mapper) {
    return parMap2(this, other.fix(PureIOOf.toPureIO()), mapper);
  }

  @Override
  default PureIO<R, E, A> repeat() {
    return repeat(1);
  }

  @Override
  default PureIO<R, E, A> repeat(int times) {
    return repeat(this, unit(), times);
  }

  @Override
  default PureIO<R, E, A> repeat(Duration delay) {
    return repeat(delay, 1);
  }

  @Override
  default PureIO<R, E, A> repeat(Duration delay, int times) {
    return repeat(this, sleep(delay), times);
  }

  @Override
  default PureIO<R, E, A> retry() {
    return retry(1);
  }

  @Override
  default PureIO<R, E, A> retry(int maxRetries) {
    return retry(this, unit(), maxRetries);
  }

  @Override
  default PureIO<R, E, A> retry(Duration delay) {
    return retry(delay, 1);
  }

  @Override
  default PureIO<R, E, A> retry(Duration delay, int maxRetries) {
    return retry(this, sleep(delay), maxRetries);
  }

  @Override
  default PureIO<R, E, Tuple2<Duration, A>> timed() {
    return PureIO.<R, E, Long>later(System::nanoTime).flatMap(
      start -> map(result -> Tuple.of(Duration.ofNanos(System.nanoTime() - start), result)));
  }

  default PureIO<R, E, Fiber<Kind<Kind<PureIO_, R>, E>, A>> fork() {
    return async((env, callback) -> {
      PureIOConnection connection = PureIOConnection.cancellable();
      Promise<Either<E, A>> promise = runAsync(env, this, connection);

      PureIO<R, E, A> join = fromPromise(promise);
      PureIO<R, E, Unit> cancel = run(connection::cancel);

      callback.accept(Try.success(Either.right(Fiber.of(join, cancel))));
    });
  }

  @Override
  default PureIO<R, E, A> timeout(Duration duration) {
    return timeout(Future.DEFAULT_EXECUTOR, duration);
  }

  default PureIO<R, E, A> timeout(Executor executor, Duration duration) {
    return racePair(executor, this, sleep(duration)).flatMap(either -> either.fold(
        ta -> ta.get2().cancel().fix(PureIOOf.toPureIO()).map(x -> ta.get1()),
        tb -> tb.get1().cancel().fix(PureIOOf.toPureIO()).flatMap(x -> PureIO.throwError(new TimeoutException()))));
  }

  @SuppressWarnings("unchecked")
  default <X extends Throwable> PureIO<R, X, A> refineOrDie(Class<X> type) {
    return flatMapError(error -> {
      if (type.isAssignableFrom(error.getClass())) {
        return PureIO.raiseError((X) error);
      }
      return PureIO.throwError(new ClassCastException(error.getClass() + " not asignable to " + type));
    });
  }

  default URIO<R, A> toURIO() {
    return new URIO<>(mapError(error -> {
      if (error instanceof Throwable throwable) {
        throw throwable;
      }
      throw new ClassCastException(error.getClass() + " is not throwable");
    }));
  }

  default RIO<R, A> toRIO() {
    return new RIO<>(refineOrDie(Throwable.class));
  }

  default Managed<R, E, A> toManaged() {
    return Managed.pure(this);
  }

  default Managed<R, E, A> toManaged(Consumer1<? super A> release) {
    return Managed.from(this, release);
  }

  static <R, E, A> PureIO<R, E, A> accessM(Function1<? super R, ? extends Kind<Kind<Kind<PureIO_, R>, E>, ? extends A>> map) {
    return new AccessM<>(value -> map.apply(value).fix(PureIOOf::narrowK));
  }

  static <R, E, A> PureIO<R, E, A> access(Function1<? super R, ? extends A> map) {
    return accessM(map.andThen(PureIO::pure));
  }

  static <R, E> PureIO<R, E, R> env() {
    return access(identity());
  }

  static <R, E> PureIO<R, E, Unit> forked(Executor executor) {
    return async((env, callback) -> executor.execute(() -> callback.accept(Try.success(Either.right(Unit.unit())))));
  }

  static <R, E, A, B, C> PureIO<R, E, C> parMap2(Kind<Kind<Kind<PureIO_, R>, E>, ? extends A> za, Kind<Kind<Kind<PureIO_, R>, E>, ? extends B> zb,
      Function2<? super A, ? super B, ? extends C> mapper) {
    return parMap2(Future.DEFAULT_EXECUTOR, za, zb, mapper);
  }

  static <R, E, A, B, C> PureIO<R, E, C> parMap2(Executor executor, Kind<Kind<Kind<PureIO_, R>, E>, ? extends A> za, Kind<Kind<Kind<PureIO_, R>, E>, ? extends B> zb,
      Function2<? super A, ? super B, ? extends C> mapper) {
    return cancellable((env, callback) -> {

      PureIOConnection connection1 = PureIOConnection.cancellable();
      PureIOConnection connection2 = PureIOConnection.cancellable();

      Promise<Either<E, A>> promiseA = runAsync(env, PureIO.<R, E>forked(executor).andThen(za), connection1);
      Promise<Either<E, B>> promiseB = runAsync(env, PureIO.<R, E>forked(executor).andThen(zb), connection2);

      promiseA.onComplete(a -> promiseB.onComplete(
        b -> callback.accept(Try.map2(a, b, (e1, e2) -> EitherOf.narrowK(Either.map2(e1, e2, mapper))))));

      return PureIO.exec(() -> {
        try {
          connection1.cancel();
        } finally {
          connection2.cancel();
        }
      });
    });
  }

  static <R, E, A, B> PureIO<R, E, Either<A, B>> race(Kind<Kind<Kind<PureIO_, R>, E>, ? extends A> fa, Kind<Kind<Kind<PureIO_, R>, E>, ? extends B> fb) {
    return race(Future.DEFAULT_EXECUTOR, fa, fb);
  }

  static <R, E, A, B> PureIO<R, E, Either<A, B>> race(Executor executor, Kind<Kind<Kind<PureIO_, R>, E>, ? extends A> fa, Kind<Kind<Kind<PureIO_, R>, E>, ? extends B> fb) {
    return racePair(executor, fa, fb).flatMap(either -> either.fold(
        ta -> ta.get2().cancel().fix(PureIOOf.toPureIO()).map(x -> Either.left(ta.get1())),
        tb -> tb.get1().cancel().fix(PureIOOf.toPureIO()).map(x -> Either.right(tb.get2()))));
  }

  static <R, E, A, B> PureIO<R, E, Either<Tuple2<A, Fiber<Kind<Kind<PureIO_, R>, E>, B>>, Tuple2<Fiber<Kind<Kind<PureIO_, R>, E>, A>, B>>>
      racePair(Executor executor, Kind<Kind<Kind<PureIO_, R>, E>, ? extends A> fa, Kind<Kind<Kind<PureIO_, R>, E>, ? extends B> fb) {
    return cancellable((env, callback) -> {

      PureIOConnection connection1 = PureIOConnection.cancellable();
      PureIOConnection connection2 = PureIOConnection.cancellable();

      Promise<Either<E, A>> promiseA = runAsync(env, PureIO.<R, E>forked(executor).andThen(fa), connection1);
      Promise<Either<E, B>> promiseB = runAsync(env, PureIO.<R, E>forked(executor).andThen(fb), connection2);

      promiseA.onComplete(result -> {
        PureIO<R, E, B> fromPromiseB = PureIO.fromPromise(promiseB);
        PureIO<R, E, Unit> cancelB = PureIO.run(connection2::cancel);
        Fiber<Kind<Kind<PureIO_, R>, E>, B> fiberB = Fiber.of(fromPromiseB, cancelB);
        callback.accept(result.map(
          either -> either.map(
            a -> Either.left(Tuple.of(a, fiberB)))));
      });

      promiseB.onComplete(result -> {
        PureIO<R, E, A> fromPromiseA = PureIO.fromPromise(promiseA);
        PureIO<R, E, Unit> cancelA = PureIO.run(connection2::cancel);
        Fiber<Kind<Kind<PureIO_, R>, E>, A> fiberA = Fiber.of(fromPromiseA, cancelA);
        callback.accept(result.map(
          either -> either.map(
            b -> Either.right(Tuple.of(fiberA, b)))));
      });

      return PureIO.exec(() -> {
        try {
          connection1.cancel();
        } finally {
          connection2.cancel();
        }
      });
    });
  }

  static <R, E, A> PureIO<R, E, A> absorb(Kind<Kind<Kind<PureIO_, R>, E>, Either<E, A>> value) {
    return value.fix(PureIOOf::narrowK).flatMap(either -> either.fold(PureIO::raiseError, PureIO::pure));
  }

  static <R, A, B> Function1<A, PureIO<R, Throwable, B>> lift(Function1<? super A, ? extends B> function) {
    return value -> pure(function.apply(value));
  }

  static <R, A, B> Function1<A, PureIO<R, Throwable, B>> liftOption(Function1<? super A, ? extends Option<? extends B>> function) {
    return value -> fromOption(function.apply(value));
  }

  static <R, A, B> Function1<A, PureIO<R, Throwable, B>> liftTry(Function1<? super A, ? extends Try<? extends B>> function) {
    return value -> fromTry(function.apply(value));
  }

  static <R, E, A, B> Function1<A, PureIO<R, E, B>> liftEither(Function1<? super A, ? extends Either<E, ? extends B>> function) {
    return value -> fromEither(function.apply(value));
  }

  static <R, A> PureIO<R, Throwable, A> fromOption(Option<? extends A> task) {
    return fromOption(cons(task));
  }

  static <R, A> PureIO<R, Throwable, A> fromOption(Producer<Option<? extends A>> task) {
    return fromEither(task.andThen(Option::toEither));
  }

  static <R, A> PureIO<R, Throwable, A> fromTry(Try<? extends A> task) {
    return fromTry(cons(task));
  }

  static <R, A> PureIO<R, Throwable, A> fromTry(Producer<Try<? extends A>> task) {
    return fromEither(task.andThen(Try::toEither));
  }

  static <R, E, A> PureIO<R, E, A> fromEither(Either<E, ? extends A> task) {
    return fromEither(cons(task));
  }

  static <R, E, A> PureIO<R, E, A> fromEither(Producer<Either<E, ? extends A>> task) {
    return new Delay<>(task);
  }

  static <R, E, A> PureIO<R, E, A> fromPromise(Promise<? extends Either<E, ? extends A>> promise) {
    Consumer1<Consumer1<? super Try<? extends Either<E, ? extends A>>>> callback = promise::onComplete;
    return async((env, cb) -> callback.accept(cb));
  }

  static <R> PureIO<R, Throwable, Unit> exec(CheckedRunnable task) {
    return new Attempt<>(task.asProducer());
  }

  static <R, E> PureIO<R, E, Unit> run(Runnable task) {
    return fromEither(() -> { task.run(); return Either.right(Unit.unit()); });
  }

  static <R, E, A> PureIO<R, E, A> pure(A value) {
    return new Pure<>(value);
  }

  static <R, E, A> PureIO<R, E, A> defer(Producer<Kind<Kind<Kind<PureIO_, R>, E>, ? extends A>> lazy) {
    return new Suspend<>(() -> lazy.get().fix(PureIOOf::narrowK));
  }

  static <R, A> PureIO<R, Throwable, A> task(Producer<? extends A> task) {
    return new Attempt<>(task);
  }

  static <R, E, A> PureIO<R, E, A> later(Producer<? extends A> task) {
    return fromEither(task.andThen(Either::right));
  }

  static <R, E, A> PureIO<R, E, A> never() {
    return async((env, cb) -> {});
  }

  static <R, E, A> PureIO<R, E, A> async(Consumer2<R, Consumer1<? super Try<? extends Either<E, ? extends A>>>> consumer) {
    return cancellable(consumer.asFunction().andThen(PureIO::pure));
  }

  static <R, E, A> PureIO<R, E, A> cancellable(Function2<R, Consumer1<? super Try<? extends Either<E, ? extends A>>>, PureIO<R, ?, Unit>> consumer) {
    return new Async<>(consumer);
  }

  static <R, E, A> PureIO<R, E, A> raiseError(E error) {
    return new Failure<>(error);
  }

  static <R, E, A> PureIO<R, E, A> throwError(Throwable error) {
    return new Throw<>(error);
  }

  static <R, A> PureIO<R, Throwable, A> redeem(Kind<Kind<Kind<PureIO_, R>, Nothing>, ? extends A> value) {
    return new Recover<>(value.fix(PureIOOf::narrowK), PartialFunction1.of(always(), PureIO::raiseError));
  }

  static <R, E> PureIO<R, E, Unit> sleep(Duration delay) {
    return sleep(Future.DEFAULT_EXECUTOR, delay);
  }

  static <R, E> PureIO<R, E, Unit> sleep(Executor executor, Duration delay) {
    return cancellable((env, callback) -> {
      Future<Unit> sleep = Future.sleep(executor, delay)
        .onComplete(result -> callback.accept(Try.success(Either.right(Unit.unit()))));
      return PureIO.exec(() -> sleep.cancel(true));
    });
  }

  static <R, E, A> PureIO<R, E, Sequence<A>> traverse(Sequence<? extends Kind<Kind<Kind<PureIO_, R>, E>, A>> sequence) {
    return traverse(Future.DEFAULT_EXECUTOR, sequence);
  }

  static <R, E, A> PureIO<R, E, Sequence<A>> traverse(Executor executor, Sequence<? extends Kind<Kind<Kind<PureIO_, R>, E>, A>> sequence) {
    return sequence.foldLeft(PureIO.<R, E, Sequence<A>>pure(ImmutableList.empty()),
        (Kind<Kind<Kind<PureIO_, R>, E>, Sequence<A>> xs, Kind<Kind<Kind<PureIO_, R>, E>, A> a) -> parMap2(executor, xs, a, Sequence::append));
  }

  static <R, E, A extends AutoCloseable, B> PureIO<R, E, B> bracket(Kind<Kind<Kind<PureIO_, R>, E>, ? extends A> acquire,
                                                                 Function1<? super A, ? extends Kind<Kind<Kind<PureIO_, R>, E>, ? extends B>> use) {
    return bracket(acquire, use, AutoCloseable::close);
  }

  static <R, E, A, B> PureIO<R, E, B> bracket(Kind<Kind<Kind<PureIO_, R>, E>, ? extends A> acquire,
                                           Function1<? super A, ? extends Kind<Kind<Kind<PureIO_, R>, E>, ? extends B>> use,
                                           Consumer1<? super A> release) {
    return bracket(acquire, use, release.asFunction().andThen(PureIO::pure));
  }

  static <R, E, A, B> PureIO<R, E, B> bracket(Kind<Kind<Kind<PureIO_, R>, E>, ? extends A> acquire,
                                           Function1<? super A, ? extends Kind<Kind<Kind<PureIO_, R>, E>, ? extends B>> use,
                                           Function1<? super A, ? extends Kind<Kind<Kind<PureIO_, R>, E>, Unit>> release) {
    // TODO: cancel
    return cancellable((env, callback) -> {

      PureIOConnection cancellable = PureIOConnection.cancellable();

      Promise<Either<E, A>> promise = runAsync(env, acquire.fix(PureIOOf::narrowK), cancellable);

      promise
        .onFailure(e -> callback.accept(Try.failure(e)))
        .onSuccess(either -> either.fold(error -> {
          callback.accept(Try.success(Either.left(error)));
          return Unit.unit();
        }, resource -> {
          Promise<Either<E, B>> runAsync = runAsync(env, use.apply(resource).fix(PureIOOf::narrowK), cancellable);

          runAsync
            .onFailure(e -> callback.accept(Try.failure(e)))
            .onSuccess(result -> {

              Promise<Either<E, Unit>> run = runAsync(env, release.apply(resource).fix(PureIOOf::narrowK), cancellable);

              run.onComplete(ignore -> result.fold(error -> {
                callback.accept(Try.success(Either.left(error)));
                return Unit.unit();
              }, b -> {
                callback.accept(Try.success(Either.right(b)));
                return Unit.unit();
              }));
          });
          return Unit.unit();
        }));

      return PureIO.run(cancellable::cancel);
    });
  }

  @SuppressWarnings("unchecked")
  static <R, E> PureIO<R, E, Unit> unit() {
    return (PureIO<R, E, Unit>) UNIT;
  }

  PureIO<?, ?, Unit> UNIT = PureIO.pure(Unit.unit());

  private static <R, E, A> Promise<Either<E, A>> runAsync(@Nullable R env, PureIO<R, E, A> current, PureIOConnection connection) {
    return runAsync(Option.of(env), current, connection, new CallStack<>(), Promise.make());
  }

  @SuppressWarnings("unchecked")
  private static <R, E, F, G, A, B, C> Promise<Either<E, A>> runAsync(
      Option<R> env, Kind<Kind<Kind<PureIO_, R>, E>, A> current, PureIOConnection connection, CallStack<R, E, A> stack, Promise<Either<E, A>> promise) {
    while (true) {
      try {
        current = unwrap(env, current, stack, identity());

        if (current instanceof Pure<R, E, A> pure) {
          return promise.succeeded(Either.right(pure.value));
        }

        if (current instanceof Failure<R, E, A> failure) {
          return promise.succeeded(Either.left(failure.error));
        }

        if (current instanceof Async<R, E, A> async) {
          return executeAsync(env, async, connection, promise);
        }

        if (current instanceof FlatMapped) {
          stack.push();

          var flatMapped = (FlatMapped<R, F, B, E, A>) current;
          Kind<Kind<Kind<PureIO_, R>, F>, B> source = unwrap(env, flatMapped.current, stack,
              b -> b.fix(PureIOOf::narrowK).foldM(flatMapped.nextError, flatMapped.next));

          if (source instanceof Async<R, F, B> async) {
            Promise<Either<F, B>> nextPromise = Promise.make();

            nextPromise.then(either -> {
              PureIO<R, E, A> fold = either.fold(
                  error -> flatMapped.nextError.apply(error).fix(PureIOOf::narrowK),
                  value -> flatMapped.next.apply(value).fix(PureIOOf::narrowK));
              runAsync(env, fold, connection, stack, promise);
            });

            executeAsync(env, async, connection, nextPromise);

            return promise;
          }

          if (source instanceof Pure<R, F, B> pure) {
            current = flatMapped.next.apply(pure.value).fix(PureIOOf::narrowK);
          } else if (source instanceof Failure<R, F, B> failure) {
            current = flatMapped.nextError.apply(failure.error).fix(PureIOOf::narrowK);
          } else if (source instanceof FlatMapped) {
            FlatMapped<R, G, C, F, B> flatMapped2 = (FlatMapped<R, G, C, F, B>) source;

            current = flatMapped2.current.fix(PureIOOf::narrowK).foldM(
                e -> flatMapped2.nextError.apply(e).fix(PureIOOf::narrowK).foldM(flatMapped.nextError, flatMapped.next),
                a -> flatMapped2.next.apply(a).fix(PureIOOf::narrowK).foldM(flatMapped.nextError, flatMapped.next));
          }
        } else {
          stack.pop();
        }
      } catch (Throwable error) {
        Option<PureIO<R, E, A>> result = stack.tryHandle(error);

        if (result.isPresent()) {
          current = result.getOrElseThrow();
        } else {
          return promise.failed(error);
        }
      }
    }
  }

  @SuppressWarnings("unchecked")
  private static <R, E, F, A, B> Kind<Kind<Kind<PureIO_, R>, E>, A> unwrap(
      Option<R> env, Kind<Kind<Kind<PureIO_, R>, E>, A> current, CallStack<R, F, B> stack,
      Function1<Kind<Kind<Kind<PureIO_, R>, E>, ? extends A>, Kind<Kind<Kind<PureIO_, R>, F>, ? extends B>> next) {
    while (true) {
      if (current instanceof Failure) {
        return current;
      } else if (current instanceof Pure) {
        return current;
      } else if (current instanceof FlatMapped) {
        return current;
      } else if (current instanceof Async) {
        return current;
      } else if (current instanceof Throw<R, E, A> throwError) {
        return stack.sneakyThrow(throwError.error);
      } else if (current instanceof Recover<R, E, A> recover) {
        stack.add(recover.mapper.andThen(next));
        current = (PureIO<R, E, A>) recover.current;
      } else if (current instanceof AccessM<R, E, A> accessM) {
        current = accessM.function.apply(env.getOrElseNull()).fix(PureIOOf::narrowK);
      } else if (current instanceof Suspend<R, E, A> suspend) {
        current = suspend.lazy.get().fix(PureIOOf::narrowK);
      } else if (current instanceof Delay<R, E, A> delay) {
        Either<E, ? extends A> value = delay.task.get();
        return value.fold(PureIO::raiseError, PureIO::pure);
      } else if (current instanceof Attempt<R, E, A> attempt) {
        Either<E, ? extends A> either = (Either<E, ? extends A>) attempt.current.liftEither().get();
        return either.fold(PureIO::raiseError, PureIO::pure);
      } else {
        throw new IllegalStateException("not supported: " + current);
      }
    }
  }

  private static <R, E, A> Promise<Either<E, A>> executeAsync(Option<R> env, Async<R, E, A> current, PureIOConnection connection, Promise<Either<E, A>> promise) {
    if (connection.isCancellable() && !connection.updateState(StateIO::startingNow).isRunnable()) {
      return promise.cancel();
    }

    connection.setCancelToken(current.callback.apply(env.getOrElseNull(), result -> promise.tryComplete(result.map(EitherOf::narrowK))));

    promise.thenRun(() -> connection.setCancelToken(UNIT));

    if (connection.isCancellable() && connection.updateState(StateIO::notStartingNow).isCancellingNow()) {
      connection.cancelNow();
    }

    return promise;
  }

  final class Pure<R, E, A> implements PureIO<R, E, A> {

    private final A value;

    private Pure(A value) {
      this.value = checkNonNull(value);
    }

    @Override
    public String toString() {
      return "Pure(" + value + ")";
    }
  }

  final class Failure<R, E, A> implements PureIO<R, E, A> {

    private final E error;

    private Failure(E error) {
      this.error = checkNonNull(error);
    }

    @Override
    public String toString() {
      return "Failure(" + error + ")";
    }
  }

  final class Throw<R, E, A> implements PureIO<R, E, A> {

    private final Throwable error;

    private Throw(Throwable error) {
      this.error = checkNonNull(error);
    }

    @Override
    public String toString() {
      return "Throw(" + error + ")";
    }
  }

  final class FlatMapped<R, E, A, F, B> implements PureIO<R, F, B> {

    private final Kind<Kind<Kind<PureIO_, R>, E>, A> current;
    private final Function1<? super E, ? extends Kind<Kind<Kind<PureIO_, R>, F>, ? extends B>> nextError;
    private final Function1<? super A, ? extends Kind<Kind<Kind<PureIO_, R>, F>, ? extends B>> next;

    private FlatMapped(PureIO<R, E, A> current,
                         Function1<? super E, ? extends Kind<Kind<Kind<PureIO_, R>, F>, ? extends B>> nextError,
                         Function1<? super A, ? extends Kind<Kind<Kind<PureIO_, R>, F>, ? extends B>> next) {
      this.current = checkNonNull(current);
      this.nextError = checkNonNull(nextError);
      this.next = checkNonNull(next);
    }

    @Override
    public String toString() {
      return "FlatMapped(" + current + ", ?, ?)";
    }
  }

  final class Delay<R, E, A> implements PureIO<R, E, A> {

    private final Producer<Either<E, ? extends A>> task;

    private Delay(Producer<Either<E, ? extends A>> task) {
      this.task = checkNonNull(task);
    }

    @Override
    public String toString() {
      return "Delay(?)";
    }
  }

  final class Suspend<R, E, A> implements PureIO<R, E, A> {

    private final Producer<Kind<Kind<Kind<PureIO_, R>, E>, ? extends A>> lazy;

    private Suspend(Producer<Kind<Kind<Kind<PureIO_, R>, E>, ? extends A>> lazy) {
      this.lazy = checkNonNull(lazy);
    }

    @Override
    public String toString() {
      return "Suspend(?)";
    }
  }

  final class Async<R, E, A> implements PureIO<R, E, A> {

    private final Function2<R, Consumer1<? super Try<? extends Either<E, ? extends A>>>, PureIO<R, ?, Unit>> callback;

    private Async(Function2<R, Consumer1<? super Try<? extends Either<E, ? extends A>>>, PureIO<R, ?, Unit>> callback) {
      this.callback = checkNonNull(callback);
    }

    @Override
    public String toString() {
      return "Async(?)";
    }
  }

  final class Attempt<R, E, A> implements PureIO<R, E, A> {

    private final Producer<? extends A> current;

    private Attempt(Producer<? extends A> current) {
      this.current = checkNonNull(current);
    }

    @Override
    public String toString() {
      return "Attempt(" + current + ")";
    }
  }

  final class Recover<R, E, A> implements PureIO<R, E, A> {

    private final PureIO<R, ?, A> current;
    private final PartialFunction1<? super Throwable, ? extends PureIO<R, E, ? extends A>> mapper;

    private Recover(PureIO<R, ?, A> current, PartialFunction1<? super Throwable, ? extends PureIO<R, E, ? extends A>> mapper) {
      this.current = checkNonNull(current);
      this.mapper = checkNonNull(mapper);
    }

    @Override
    public String toString() {
      return "Recover(" + current + ", ?)";
    }
  }

  final class AccessM<R, E, A> implements PureIO<R, E, A> {

    private final Function1<? super R, ? extends Kind<Kind<Kind<PureIO_, R>, E>, ? extends A>> function;

    private AccessM(Function1<? super R, ? extends Kind<Kind<Kind<PureIO_, R>, E>, ? extends A>> function) {
      this.function = checkNonNull(function);
    }

    @Override
    public String toString() {
      return "AccessM(?)";
    }
  }

  private static <R, E, A> PureIO<R, E, A> repeat(PureIO<R, E, A> self, PureIO<R, E, Unit> pause, int times) {
    return self.foldM(PureIO::raiseError, value -> {
      if (times > 0) {
        return pause.andThen(repeat(self, pause, times - 1));
      }
      return PureIO.pure(value);
    });
  }

  private static <R, E, A> PureIO<R, E, A> retry(PureIO<R, E, A> self, PureIO<R, E, Unit> pause, int maxRetries) {
    return self.foldM(error -> {
      if (maxRetries > 0) {
        return pause.andThen(retry(self, pause.repeat(), maxRetries - 1));
      }
      return PureIO.raiseError(error);
    }, PureIO::pure);
  }
}

sealed interface PureIOConnection {

  PureIOConnection UNCANCELLABLE = new Uncancellable();

  boolean isCancellable();

  void setCancelToken(PureIO<?, ?, Unit> cancel);

  void cancelNow();

  void cancel();

  StateIO updateState(Operator1<StateIO> update);

  static PureIOConnection cancellable() {
    return new Cancellable();
  }

  final class Uncancellable implements PureIOConnection {

    private Uncancellable() { }

    @Override
    public boolean isCancellable() {
      return false;
    }

    @Override
    public void setCancelToken(PureIO<?, ?, Unit> cancel) {
      /* nothing to do */
    }

    @Override
    public void cancelNow() {
      /* nothing to do */
    }

    @Override
    public void cancel() {
      /* nothing to do */
    }

    @Override
    public StateIO updateState(Operator1<StateIO> update) {
      return StateIO.INITIAL;
    }
  }

  final class Cancellable implements PureIOConnection {

    private Cancellable() { }

    private PureIO<?, ?, Unit> cancelToken = PureIO.UNIT;
    private final AtomicReference<StateIO> state = new AtomicReference<>(StateIO.INITIAL);

    @Override
    public boolean isCancellable() {
      return true;
    }

    @Override
    public void setCancelToken(PureIO<?, ?, Unit> cancel) {
      this.cancelToken = checkNonNull(cancel);
    }

    @Override
    public void cancelNow() {
      cancelToken.fix(PureIOOf::narrowK).runAsync(null);
    }

    @Override
    public void cancel() {
      if (state.getAndUpdate(StateIO::cancellingNow).isCancelable()) {
        cancelNow();

        state.set(StateIO.CANCELLED);
      }
    }

    @Override
    public StateIO updateState(Operator1<StateIO> update) {
      return state.updateAndGet(update::apply);
    }
  }
}

record StateIO(boolean isCancelled, boolean isCancellingNow, boolean isStartingNow) {

  static final StateIO INITIAL = new StateIO(false, false, false);
  static final StateIO CANCELLED = new StateIO(true, false, false);

  StateIO cancellingNow() {
    return new StateIO(isCancelled, true, isStartingNow);
  }

  StateIO startingNow() {
    return new StateIO(isCancelled, isCancellingNow, true);
  }

  StateIO notStartingNow() {
    return new StateIO(isCancelled, isCancellingNow, false);
  }

  boolean isCancelable() {
    return !isCancelled && !isCancellingNow && !isStartingNow;
  }

  boolean isRunnable() {
    return !isCancelled && !isCancellingNow;
  }
}

final class CallStack<R, E, A> implements Recoverable {

  @Nullable
  private StackItem<R, E, A> top = new StackItem<>();

  public void push() {
    if (top != null) {
      top.push();
    }
  }

  public void pop() {
    if (top == null) {
      return;
    }
    if (top.count() > 0) {
      top.pop();
    } else {
      top = top.prev();
    }
  }

  public void add(PartialFunction1<? super Throwable, ? extends Kind<Kind<Kind<PureIO_, R>, E>, ? extends A>> mapError) {
    if (top == null) {
      return;
    }
    if (top.count() > 0) {
      top.pop();
      top = new StackItem<>(top);
    }
    top.add(mapError);
  }

  public Option<PureIO<R, E, A>> tryHandle(Throwable error) {
    while (top != null) {
      top.reset();
      Option<PureIO<R, E, A>> result = top.tryHandle(error);

      if (result.isPresent()) {
        return result;
      } else {
        top = top.prev();
      }
    }
    return Option.none();
  }
}

final class StackItem<R, E, A> {

  private int count = 0;
  private final Deque<PartialFunction1<? super Throwable, ? extends Kind<Kind<Kind<PureIO_, R>, E>, ? extends A>>> recover = new ArrayDeque<>();

  @Nullable
  private final StackItem<R, E, A> prev;

  public StackItem() {
    this(null);
  }

  public StackItem(@Nullable StackItem<R, E, A> prev) {
    this.prev = prev;
  }

  @Nullable
  public StackItem<R, E, A> prev() {
    return prev;
  }

  public int count() {
    return count;
  }

  public void push() {
    count++;
  }

  public void pop() {
    count--;
  }

  public void reset() {
    count = 0;
  }

  public void add(PartialFunction1<? super Throwable, ? extends Kind<Kind<Kind<PureIO_, R>, E>, ? extends A>> mapError) {
    recover.addFirst(mapError);
  }

  public Option<PureIO<R, E, A>> tryHandle(Throwable error) {
    while (!recover.isEmpty()) {
      var mapError = recover.removeFirst();
      if (mapError.isDefinedAt(error)) {
        return Option.some(mapError.andThen(PureIOOf::<R, E, A>narrowK).apply(error));
      }
    }
    return Option.none();
  }
}