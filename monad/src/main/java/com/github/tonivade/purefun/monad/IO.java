/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.monad;

import static com.github.tonivade.purefun.core.Function1.identity;
import static com.github.tonivade.purefun.core.Matcher1.always;
import static com.github.tonivade.purefun.core.Precondition.checkNonNull;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.concurrent.Future;
import com.github.tonivade.purefun.concurrent.Promise;
import com.github.tonivade.purefun.core.CheckedRunnable;
import com.github.tonivade.purefun.core.Consumer1;
import com.github.tonivade.purefun.core.Effect;
import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Function2;
import com.github.tonivade.purefun.core.Operator1;
import com.github.tonivade.purefun.core.PartialFunction1;
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

@HigherKind
public sealed interface IO<T> extends IOOf<T>, Effect<IO_, T>, Recoverable {

  IO<Unit> UNIT = pure(Unit.unit());

  default Future<T> runAsync() {
    return Future.from(runAsync(this, IOConnection.UNCANCELLABLE));
  }

  default Future<T> runAsync(Executor executor) {
    return forked(executor).andThen(this).runAsync();
  }

  default T unsafeRunSync() {
    return safeRunSync().getOrElseThrow();
  }

  default Try<T> safeRunSync() {
    return runAsync().await();
  }

  default void safeRunAsync(Consumer1<? super Try<? extends T>> callback) {
    safeRunAsync(Future.DEFAULT_EXECUTOR, callback);
  }

  default void safeRunAsync(Executor executor, Consumer1<? super Try<? extends T>> callback) {
    runAsync(executor).onComplete(callback);
  }

  @Override
  default <R> IO<R> map(Function1<? super T, ? extends R> map) {
    return flatMap(map.andThen(IO::pure));
  }

  @Override
  default <R> IO<R> flatMap(Function1<? super T, ? extends Kind<IO_, ? extends R>> map) {
    return new FlatMapped<>(this, map);
  }

  @Override
  default <R> IO<R> andThen(Kind<IO_, ? extends R> after) {
    return flatMap(ignore -> after);
  }

  @Override
  default <R> IO<R> ap(Kind<IO_, Function1<? super T, ? extends R>> apply) {
    return parMap2(Future.DEFAULT_EXECUTOR, this, apply, (v, a) -> a.apply(v));
  }

  default IO<Try<T>> attempt() {
    return map(Try::success).recover(Try::failure);
  }

  default IO<Either<Throwable, T>> either() {
    return attempt().map(Try::toEither);
  }

  default <L, R> IO<Either<L, R>> either(Function1<? super Throwable, ? extends L> mapError,
                                         Function1<? super T, ? extends R> mapper) {
    return either().map(either -> either.bimap(mapError, mapper));
  }

  default <R> IO<R> redeem(Function1<? super Throwable, ? extends R> mapError,
                           Function1<? super T, ? extends R> mapper) {
    return attempt().map(result -> result.fold(mapError, mapper));
  }

  default <R> IO<R> redeemWith(Function1<? super Throwable, ? extends Kind<IO_, ? extends R>> mapError,
                               Function1<? super T, ? extends Kind<IO_, ? extends R>> mapper) {
    return attempt().flatMap(result -> result.fold(mapError, mapper));
  }

  default IO<T> recover(Function1<? super Throwable, ? extends T> mapError) {
    return recoverWith(PartialFunction1.of(always(), mapError.andThen(IO::pure)));
  }

  @SuppressWarnings("unchecked")
  default <X extends Throwable> IO<T> recover(Class<X> type, Function1<? super X, ? extends T> function) {
    return recoverWith(PartialFunction1.of(error -> error.getClass().equals(type), t -> function.andThen(IO::pure).apply((X) t)));
  }

  default IO<T> recoverWith(PartialFunction1<? super Throwable, ? extends Kind<IO_, ? extends T>> mapper) {
    return new Recover<>(this, mapper.andThen(IOOf::narrowK));
  }

  @Override
  default IO<Tuple2<Duration, T>> timed() {
    return IO.task(System::nanoTime).flatMap(
      start -> map(result -> Tuple.of(Duration.ofNanos(System.nanoTime() - start), result)));
  }

  default IO<Fiber<IO_, T>> fork() {
    return async(callback -> {
      IOConnection connection = IOConnection.cancellable();
      Promise<T> promise = runAsync(this, connection);

      IO<T> join = fromPromise(promise);
      IO<Unit> cancel = exec(connection::cancel);

      callback.accept(Try.success(Fiber.of(join, cancel)));
    });
  }

  default IO<T> timeout(Duration duration) {
    return timeout(Future.DEFAULT_EXECUTOR, duration);
  }

  default IO<T> timeout(Executor executor, Duration duration) {
    return racePair(executor, this, sleep(duration)).flatMap(either -> either.fold(
        ta -> ta.get2().cancel().fix(IOOf.toIO()).map(x -> ta.get1()),
        tb -> tb.get1().cancel().fix(IOOf.toIO()).flatMap(x -> IO.raiseError(new TimeoutException()))));
  }

  @Override
  default IO<T> repeat() {
    return repeat(1);
  }

  @Override
  default IO<T> repeat(int times) {
    return repeat(this, unit(), times);
  }

  @Override
  default IO<T> repeat(Duration delay) {
    return repeat(delay, 1);
  }

  @Override
  default IO<T> repeat(Duration delay, int times) {
    return repeat(this, sleep(delay), times);
  }

  @Override
  default IO<T> retry() {
    return retry(1);
  }

  @Override
  default IO<T> retry(int maxRetries) {
    return retry(this, unit(), maxRetries);
  }

  @Override
  default IO<T> retry(Duration delay) {
    return retry(delay, 1);
  }

  @Override
  default IO<T> retry(Duration delay, int maxRetries) {
    return retry(this, sleep(delay), maxRetries);
  }

  static <T> IO<T> pure(T value) {
    return new Pure<>(value);
  }

  static <A, B> IO<Either<A, B>> race(Kind<IO_, ? extends A> fa, Kind<IO_, ? extends B> fb) {
    return race(Future.DEFAULT_EXECUTOR, fa, fb);
  }

  static <A, B> IO<Either<A, B>> race(Executor executor, Kind<IO_, ? extends A> fa, Kind<IO_, ? extends B> fb) {
    return racePair(executor, fa, fb).flatMap(either -> either.fold(
        ta -> ta.get2().cancel().fix(IOOf.toIO()).map(x -> Either.left(ta.get1())),
        tb -> tb.get1().cancel().fix(IOOf.toIO()).map(x -> Either.right(tb.get2()))));
  }

  static <A, B> IO<Either<Tuple2<A, Fiber<IO_, B>>, Tuple2<Fiber<IO_, A>, B>>> racePair(Executor executor, Kind<IO_, ? extends A> fa, Kind<IO_, ? extends B> fb) {
    return cancellable(callback -> {

      IOConnection connection1 = IOConnection.cancellable();
      IOConnection connection2 = IOConnection.cancellable();

      Promise<A> promiseA = runAsync(IO.forked(executor).andThen(fa), connection1);
      Promise<B> promiseB = runAsync(IO.forked(executor).andThen(fb), connection2);

      promiseA.onComplete(result -> callback.accept(
          result.map(a -> Either.left(Tuple.of(a, Fiber.of(IO.fromPromise(promiseB), IO.exec(connection2::cancel)))))));
      promiseB .onComplete(result -> callback.accept(
          result.map(b -> Either.right(Tuple.of(Fiber.of(IO.fromPromise(promiseA), IO.exec(connection2::cancel)), b)))));

      return IO.exec(() -> {
        try {
          connection1.cancel();
        } finally {
          connection2.cancel();
        }
      });
    });
  }

  static <T> IO<T> raiseError(Throwable error) {
    return new Failure<>(error);
  }

  static <T> IO<T> delay(Duration delay, Producer<? extends T> lazy) {
    return sleep(delay).andThen(task(lazy));
  }

  static <T> IO<T> suspend(Producer<? extends Kind<IO_, ? extends T>> lazy) {
    return new Suspend<>(() -> lazy.get().fix(IOOf::narrowK));
  }

  static <T, R> Function1<T, IO<R>> lift(Function1<T, R> task) {
    return task.andThen(IO::pure);
  }

  static <A, B> Function1<A, IO<B>> liftOption(Function1<? super A, ? extends Option<? extends B>> function) {
    return value -> fromOption(function.apply(value));
  }

  static <A, B> Function1<A, IO<B>> liftTry(Function1<? super A, ? extends Try<? extends B>> function) {
    return value -> fromTry(function.apply(value));
  }

  static <A, B> Function1<A, IO<B>> liftEither(Function1<? super A, ? extends Either<Throwable, ? extends B>> function) {
    return value -> fromEither(function.apply(value));
  }

  static <T> IO<T> fromOption(Option<? extends T> task) {
    return fromEither(task.toEither());
  }

  static <T> IO<T> fromTry(Try<? extends T> task) {
    return fromEither(task.toEither());
  }

  static <T> IO<T> fromEither(Either<Throwable, ? extends T> task) {
    return task.fold(IO::raiseError, IO::pure);
  }

  static <T> IO<T> fromPromise(Promise<? extends T> promise) {
    Consumer1<Consumer1<? super Try<? extends T>>> callback = promise::onComplete;
    return async(callback);
  }

  static <T> IO<T> fromCompletableFuture(CompletableFuture<? extends T> promise) {
    return fromPromise(Promise.from(promise));
  }

  static IO<Unit> sleep(Duration duration) {
    return sleep(Future.DEFAULT_EXECUTOR, duration);
  }

  static IO<Unit> sleep(Executor executor, Duration duration) {
    return cancellable(callback -> {
      Future<Unit> sleep = Future.sleep(executor, duration)
        .onComplete(result -> callback.accept(Try.success(Unit.unit())));
      return IO.exec(() -> sleep.cancel(true));
    });
  }

  static IO<Unit> exec(CheckedRunnable task) {
    return task(task.asProducer());
  }

  static <T> IO<T> task(Producer<? extends T> producer) {
    return new Delay<>(producer);
  }

  static <T> IO<T> never() {
    return async(callback -> {});
  }

  static IO<Unit> forked() {
    return forked(Future.DEFAULT_EXECUTOR);
  }

  static IO<Unit> forked(Executor executor) {
    return async(callback -> executor.execute(() -> callback.accept(Try.success(Unit.unit()))));
  }

  static <T> IO<T> async(Consumer1<Consumer1<? super Try<? extends T>>> callback) {
    return cancellable(callback.asFunction().andThen(IO::pure));
  }

  static <T> IO<T> cancellable(Function1<Consumer1<? super Try<? extends T>>, Kind<IO_, Unit>> callback) {
    return new Async<>(callback);
  }

  static <A, T> IO<Function1<A, IO<T>>> memoize(Function1<A, IO<T>> function) {
    return memoize(Future.DEFAULT_EXECUTOR, function);
  }

  static <A, T> IO<Function1<A, IO<T>>> memoize(Executor executor, Function1<A, IO<T>> function) {
    var ref = Ref.make(ImmutableMap.<A, Promise<T>>empty());
    return ref.map(r -> {
      Function1<A, IO<IO<T>>> result = a -> r.modify(map -> map.get(a).fold(() -> {
        Promise<T> promise = Promise.make();
        function.apply(a).safeRunAsync(executor, promise::tryComplete);
        return Tuple.of(IO.fromPromise(promise), map.put(a, promise));
      }, promise -> Tuple.of(IO.fromPromise(promise), map)));
      return result.andThen(io -> io.flatMap(identity()));
    });
  }

  static IO<Unit> unit() {
    return UNIT;
  }

  static <T, R> IO<R> bracket(Kind<IO_, ? extends T> acquire,
      Function1<? super T, ? extends Kind<IO_, ? extends R>> use, Function1<? super T, ? extends Kind<IO_, Unit>> release) {
    return cancellable(callback -> {

      IOConnection cancellable = IOConnection.cancellable();

      Promise<? extends T> promise = runAsync(acquire.fix(IOOf::narrowK), cancellable);

      promise
        .onFailure(error -> callback.accept(Try.failure(error)))
        .onSuccess(resource -> runAsync(use.apply(resource).fix(IOOf::narrowK), cancellable)
          .onComplete(result -> runAsync(release.apply(resource).fix(IOOf::narrowK), cancellable)
            .onComplete(ignore -> callback.accept(result))
        ));

      return IO.exec(cancellable::cancel);
    });
  }

  static <T, R> IO<R> bracket(Kind<IO_, ? extends T> acquire,
      Function1<? super T, ? extends Kind<IO_, ? extends R>> use, Consumer1<? super T> release) {
    return bracket(acquire, use, release.asFunction().andThen(IO::pure));
  }

  static <T extends AutoCloseable, R> IO<R> bracket(Kind<IO_, ? extends T> acquire,
      Function1<? super T, ? extends Kind<IO_, ? extends R>> use) {
    return bracket(acquire, use, AutoCloseable::close);
  }

  static IO<Unit> sequence(Sequence<? extends Kind<IO_, ?>> sequence) {
    Kind<IO_, ?> initial = IO.unit().kind();
    return sequence.foldLeft(initial,
        (Kind<IO_, ?> a, Kind<IO_, ?> b) -> a.fix(IOOf::narrowK).andThen(b.fix(IOOf::narrowK))).fix(IOOf::narrowK).andThen(IO.unit());
  }

  static <A> IO<Sequence<A>> traverse(Sequence<? extends Kind<IO_, A>> sequence) {
    return traverse(Future.DEFAULT_EXECUTOR, sequence);
  }

  static <A> IO<Sequence<A>> traverse(Executor executor, Sequence<? extends Kind<IO_, A>> sequence) {
    return sequence.foldLeft(pure(ImmutableList.empty()),
        (Kind<IO_, Sequence<A>> xs, Kind<IO_, A> a) -> parMap2(executor, xs, a, Sequence::append));
  }

  static <A, B, C> IO<C> parMap2(Kind<IO_, ? extends A> fa, Kind<IO_, ? extends B> fb,
                              Function2<? super A, ? super B, ? extends C> mapper) {
    return parMap2(Future.DEFAULT_EXECUTOR, fa, fb, mapper);
  }

  static <A, B, C> IO<C> parMap2(Executor executor, Kind<IO_, ? extends A> fa, Kind<IO_, ? extends B> fb,
                              Function2<? super A, ? super B, ? extends C> mapper) {
    return cancellable(callback -> {

      IOConnection connection1 = IOConnection.cancellable();
      IOConnection connection2 = IOConnection.cancellable();

      Promise<A> promiseA = runAsync(IO.forked(executor).andThen(fa), connection1);
      Promise<B> promiseB = runAsync(IO.forked(executor).andThen(fb), connection2);

      promiseA.onComplete(a -> promiseB.onComplete(b -> callback.accept(Try.map2(a, b, mapper))));

      return IO.exec(() -> {
        try {
          connection1.cancel();
        } finally {
          connection2.cancel();
        }
      });
    });
  }

  static <A, B> IO<Tuple2<A, B>> tuple(Kind<IO_, ? extends A> fa, Kind<IO_, ? extends B> fb) {
    return tuple(Future.DEFAULT_EXECUTOR, fa, fb);
  }

  static <A, B> IO<Tuple2<A, B>> tuple(Executor executor, Kind<IO_, ? extends A> fa, Kind<IO_, ? extends B> fb) {
    return parMap2(executor, fa, fb, Tuple::of);
  }

  private static <T> Promise<T> runAsync(IO<T> current, IOConnection connection) {
    return runAsync(current, connection, new CallStack<>(), Promise.make());
  }

  @SuppressWarnings("unchecked")
  private static <T, U, V> Promise<T> runAsync(Kind<IO_, T> current, IOConnection connection, CallStack<T> stack, Promise<T> promise) {
    while (true) {
      try {
        current = unwrap(current, stack, identity());

        if (current instanceof Pure<T> pure) {
          return promise.succeeded(pure.value);
        }

        if (current instanceof Async<T> async) {
          return executeAsync(async, connection, promise);
        }

        if (current instanceof FlatMapped) {
          stack.push();

          var flatMapped = (FlatMapped<U, T>) current;
          IO<U> source = unwrap(flatMapped.current, stack, u -> u.fix(IOOf::narrowK).flatMap(flatMapped.next)).fix(IOOf::narrowK);

          if (source instanceof Async<U> async) {
            Promise<U> nextPromise = Promise.make();

            nextPromise.then(u -> runAsync(flatMapped.next.apply(u).fix(IOOf::narrowK), connection, stack, promise));

            executeAsync(async, connection, nextPromise);

            return promise;
          }

          if (source instanceof Pure<U> pure) {
            current = flatMapped.next.apply(pure.value).fix(IOOf::narrowK);
          } else if (source instanceof FlatMapped) {
            var flatMapped2 = (FlatMapped<V, U>) source;
            current = flatMapped2.current.fix(IOOf::narrowK)
                .flatMap(a -> flatMapped2.next.apply(a).fix(IOOf::narrowK)
                    .flatMap(flatMapped.next));
          }
        } else {
          stack.pop();
        }
      } catch (Throwable error) {
        Option<IO<T>> result = stack.tryHandle(error);

        if (result.isPresent()) {
          current = result.getOrElseThrow();
        } else {
          return promise.failed(error);
        }
      }
    }
  }

  private static <T, U> Kind<IO_,T> unwrap(Kind<IO_, T> current, CallStack<U> stack, Function1<Kind<IO_, ? extends T>, Kind<IO_, ? extends U>> next) {
    while (true) {
      if (current instanceof Failure<T> failure) {
        return stack.sneakyThrow(failure.error);
      } else if (current instanceof Recover<T> recover) {
        stack.add(recover.mapper.andThen(next));
        current = recover.current;
      } else if (current instanceof Suspend<T> suspend) {
        current = suspend.lazy.get().fix(IOOf::narrowK);
      } else if (current instanceof Delay<T> delay) {
        return IO.pure(delay.task.get());
      } else if (current instanceof Pure) {
        return current;
      } else if (current instanceof FlatMapped) {
        return current;
      } else if (current instanceof Async) {
        return current;
      } else {
        throw new IllegalStateException();
      }
    }
  }

  private static <T> Promise<T> executeAsync(Async<T> current, IOConnection connection, Promise<T> promise) {
    if (connection.isCancellable() && !connection.updateState(StateIO::startingNow).isRunnable()) {
      return promise.cancel();
    }

    connection.setCancelToken(current.callback.apply(promise::tryComplete));

    promise.thenRun(() -> connection.setCancelToken(UNIT));

    if (connection.isCancellable() && connection.updateState(StateIO::notStartingNow).isCancellingNow()) {
      connection.cancelNow();
    }

    return promise;
  }

  private static <T> IO<T> repeat(IO<T> self, IO<Unit> pause, int times) {
    return self.redeemWith(IO::raiseError, value -> {
      if (times > 0) {
        return pause.andThen(repeat(self, pause, times - 1));
      }
      return IO.pure(value);
    });
  }

  private static <T> IO<T> retry(IO<T> self, IO<Unit> pause, int maxRetries) {
    return self.redeemWith(error -> {
      if (maxRetries > 0) {
        return pause.andThen(retry(self, pause.repeat(), maxRetries - 1));
      }
      return IO.raiseError(error);
    }, IO::pure);
  }

  final class Pure<T> implements IO<T> {

    private final T value;

    private Pure(T value) {
      this.value = checkNonNull(value);
    }

    @Override
    public String toString() {
      return "Pure(" + value + ")";
    }
  }

  final class Failure<T> implements IO<T>, Recoverable {

    private final Throwable error;

    private Failure(Throwable error) {
      this.error = checkNonNull(error);
    }

    @Override
    public String toString() {
      return "Failure(" + error + ")";
    }
  }

  final class FlatMapped<T, R> implements IO<R> {

    private final Kind<IO_, ? extends T> current;
    private final Function1<? super T, ? extends Kind<IO_, ? extends R>> next;

    private FlatMapped(IO<? extends T> current,
                         Function1<? super T, ? extends Kind<IO_, ? extends R>> next) {
      this.current = checkNonNull(current);
      this.next = checkNonNull(next);
    }

    @Override
    public String toString() {
      return "FlatMapped(" + current + ", ?)";
    }
  }

  final class Delay<T> implements IO<T> {

    private final Producer<? extends T> task;

    private Delay(Producer<? extends T> task) {
      this.task = checkNonNull(task);
    }

    @Override
    public String toString() {
      return "Delay(?)";
    }
  }

  final class Async<T> implements IO<T> {

    private final Function1<Consumer1<? super Try<? extends T>>, Kind<IO_, Unit>> callback;

    private Async(Function1<Consumer1<? super Try<? extends T>>, Kind<IO_, Unit>> callback) {
      this.callback = checkNonNull(callback);
    }

    @Override
    public String toString() {
      return "Async(?)";
    }
  }

  final class Suspend<T> implements IO<T> {

    private final Producer<? extends Kind<IO_, ? extends T>> lazy;

    private Suspend(Producer<? extends Kind<IO_, ? extends T>> lazy) {
      this.lazy = checkNonNull(lazy);
    }

    @Override
    public String toString() {
      return "Suspend(?)";
    }
  }

  final class Recover<T> implements IO<T> {

    private final Kind<IO_, T> current;
    private final PartialFunction1<? super Throwable, ? extends Kind<IO_, ? extends T>> mapper;

    private Recover(IO<T> current, PartialFunction1<? super Throwable, ? extends Kind<IO_, ? extends T>> mapper) {
      this.current = checkNonNull(current);
      this.mapper = checkNonNull(mapper);
    }

    @Override
    public String toString() {
      return "Recover(" + current + ", ?)";
    }
  }
}

sealed interface IOConnection {

  IOConnection UNCANCELLABLE = new Uncancellable();

  boolean isCancellable();

  void setCancelToken(Kind<IO_, Unit> cancel);

  void cancelNow();

  void cancel();

  StateIO updateState(Operator1<StateIO> update);

  static IOConnection cancellable() {
    return new Cancellable();
  }

  final class Uncancellable implements IOConnection {

    private Uncancellable() { }

    @Override
    public boolean isCancellable() {
      return false;
    }

    @Override
    public void setCancelToken(Kind<IO_, Unit> cancel) {
      // uncancellable
    }

    @Override
    public void cancelNow() {
      // uncancellable
    }

    @Override
    public void cancel() {
      // uncancellable
    }

    @Override
    public StateIO updateState(Operator1<StateIO> update) {
      return StateIO.INITIAL;
    }
  }

  final class Cancellable implements IOConnection {

    private Kind<IO_, Unit> cancelToken;
    private final AtomicReference<StateIO> state = new AtomicReference<>(StateIO.INITIAL);

    private Cancellable() { }

    @Override
    public boolean isCancellable() {
      return true;
    }

    @Override
    public void setCancelToken(Kind<IO_, Unit> cancel) {
      this.cancelToken = checkNonNull(cancel);
    }

    @Override
    public void cancelNow() {
      cancelToken.fix(IOOf::narrowK).runAsync();
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

final class StateIO {

  public static final StateIO INITIAL = new StateIO(false, false, false);
  public static final StateIO CANCELLED = new StateIO(true, false, false);

  private final boolean isCancelled;
  private final boolean cancellingNow;
  private final boolean startingNow;

  public StateIO(boolean isCancelled, boolean cancellingNow, boolean startingNow) {
    this.isCancelled = isCancelled;
    this.cancellingNow = cancellingNow;
    this.startingNow = startingNow;
  }

  public boolean isCancelled() {
    return isCancelled;
  }

  public boolean isCancellingNow() {
    return cancellingNow;
  }

  public boolean isStartingNow() {
    return startingNow;
  }

  public StateIO cancellingNow() {
    return new StateIO(isCancelled, true, startingNow);
  }

  public StateIO startingNow() {
    return new StateIO(isCancelled, cancellingNow, true);
  }

  public StateIO notStartingNow() {
    return new StateIO(isCancelled, cancellingNow, false);
  }

  public boolean isCancelable() {
    return !isCancelled && !cancellingNow && !startingNow;
  }

  public boolean isRunnable() {
    return !isCancelled && !cancellingNow;
  }
}

final class CallStack<T> implements Recoverable {

  private StackItem<T> top = new StackItem<>();

  public void push() {
    top.push();
  }

  public void pop() {
    if (top.count() > 0) {
      top.pop();
    } else {
      top = top.prev();
    }
  }

  public void add(PartialFunction1<? super Throwable, ? extends Kind<IO_, ? extends T>> mapError) {
    if (top.count() > 0) {
      top.pop();
      top = new StackItem<>(top);
    }
    top.add(mapError);
  }

  public Option<IO<T>> tryHandle(Throwable error) {
    while (top != null) {
      top.reset();
      Option<IO<T>> result = top.tryHandle(error);

      if (result.isPresent()) {
        return result;
      } else {
        top = top.prev();
      }
    }
    return Option.none();
  }
}

final class StackItem<T> {

  private int count = 0;
  private final Deque<PartialFunction1<? super Throwable, ? extends Kind<IO_, ? extends T>>> recover = new ArrayDeque<>();

  private final StackItem<T> prev;

  public StackItem() {
    this(null);
  }

  public StackItem(StackItem<T> prev) {
    this.prev = prev;
  }

  public StackItem<T> prev() {
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

  public void add(PartialFunction1<? super Throwable, ? extends Kind<IO_, ? extends T>> mapError) {
    recover.addFirst(mapError);
  }

  public Option<IO<T>> tryHandle(Throwable error) {
    while (!recover.isEmpty()) {
      var mapError = recover.removeFirst();
      if (mapError.isDefinedAt(error)) {
        return Option.some(mapError.apply(error).fix(IOOf::narrowK));
      }
    }
    return Option.none();
  }
}