/*
 * Copyright (c) 2018-2021, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.monad;

import static com.github.tonivade.purefun.Function1.identity;
import static com.github.tonivade.purefun.Matcher1.always;
import static com.github.tonivade.purefun.Precondition.checkNonNull;
import java.time.Duration;
import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import com.github.tonivade.purefun.CheckedRunnable;
import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Effect;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Function2;
import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Operator1;
import com.github.tonivade.purefun.PartialFunction1;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Recoverable;
import com.github.tonivade.purefun.Tuple;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.concurrent.Future;
import com.github.tonivade.purefun.concurrent.Promise;
import com.github.tonivade.purefun.data.ImmutableList;
import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.monad.IO.FlatMapped;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.type.Try;

@HigherKind(sealed = true)
public interface IO<T> extends IOOf<T>, Effect<IO_, T>, Recoverable {

  default Future<T> runAsync() {
    return Future.from(IOModule.runAsync(this, IOConnection.UNCANCELLABLE));
  }
  
  default T unsafeRunSync() {
    return safeRunSync().getOrElseThrow();
  }

  default Try<T> safeRunSync() {
    return runAsync().await();
  }

  default void safeRunAsync(Consumer1<? super Try<? extends T>> callback) {
    runAsync().onComplete(callback);
  }

  @Override
  default <R> IO<R> map(Function1<? super T, ? extends R> map) {
    return flatMap(map.andThen(IO::pure));
  }

  @Override
  default <R> IO<R> flatMap(Function1<? super T, ? extends Kind<IO_, ? extends R>> map) {
    return new FlatMapped<>(this, map.andThen(IOOf::narrowK));
  }

  @Override
  default <R> IO<R> andThen(Kind<IO_, ? extends R> after) {
    return flatMap(ignore -> after);
  }

  @Override
  default <R> IO<R> ap(Kind<IO_, Function1<? super T, ? extends R>> apply) {
    throw new UnsupportedOperationException();
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
    throw new UnsupportedOperationException();
  }
  
  default IO<Unit> fork(Executor executor) {
    return this.andThen(IO.forked(executor));
  }

  @Override
  default IO<T> repeat() {
    return repeat(1);
  }

  @Override
  default IO<T> repeat(int times) {
    return IOModule.repeat(this, unit(), times);
  }

  @Override
  default IO<T> repeat(Duration delay) {
    return repeat(delay, 1);
  }

  @Override
  default IO<T> repeat(Duration delay, int times) {
    return IOModule.repeat(this, sleep(delay), times);
  }

  @Override
  default IO<T> retry() {
    return retry(1);
  }

  @Override
  default IO<T> retry(int maxRetries) {
    return IOModule.retry(this, unit(), maxRetries);
  }

  @Override
  default IO<T> retry(Duration delay) {
    return retry(delay, 1);
  }

  @Override
  default IO<T> retry(Duration delay, int maxRetries) {
    return IOModule.retry(this, sleep(delay), maxRetries);
  }

  static <T> IO<T> pure(T value) {
    return new Pure<>(value);
  }
  
  static <A, B> IO<Either<A, B>> race(IO<A> fa, IO<B> fb) {
    return race(Future.DEFAULT_EXECUTOR, fa, fb);
  }
  
  static <A, B> IO<Either<A, B>> race(Executor executor, IO<A> fa, IO<B> fb) {
    return racePair(executor, fa, fb).flatMap(either -> either.fold(
        ta -> ta.get2().map(x -> Either.left(ta.get1())),
        tb -> tb.get1().map(x -> Either.right(tb.get2()))));
  }
  
  static <A, B> IO<Either<Tuple2<A, IO<Unit>>, Tuple2<IO<Unit>, B>>> racePair(Executor executor, IO<A> fa, IO<B> fb) {
    return cancellable(callback -> {
      
      IOConnection connection1 = IOConnection.cancellable();
      IOConnection connection2 = IOConnection.cancellable();
      
      IOModule.runAsync(IO.forked(executor).andThen(fa), connection1)
        .onComplete(result -> callback.accept(
          result.map(a -> Either.left(Tuple.of(a, IO.exec(connection2::cancel))))));
      IOModule.runAsync(IO.forked(executor).andThen(fb), connection2)
        .onComplete(result -> callback.accept(
          result.map(b -> Either.right(Tuple.of(IO.exec(connection2::cancel), b)))));

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

  static <T> IO<T> suspend(Producer<? extends IO<? extends T>> lazy) {
    return new Suspend<>(lazy);
  }

  static <T, R> Function1<T, IO<R>> lift(Function1<T, R> task) {
    return task.andThen(IO::pure);
  }

  public static <A, B> Function1<A, IO<B>> liftOption(Function1<? super A, Option<? extends B>> function) {
    return value -> fromOption(function.apply(value));
  }

  public static <A, B> Function1<A, IO<B>> liftTry(Function1<? super A, Try<? extends B>> function) {
    return value -> fromTry(function.apply(value));
  }

  public static <A, B> Function1<A, IO<B>> liftEither(Function1<? super A, Either<Throwable, ? extends B>> function) {
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

  static IO<Unit> sleep(Duration duration) {
    return cancellable(callback -> {
      Future<Unit> sleep = Future.sleep(duration)
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

  static <T> IO<T> cancellable(Function1<Consumer1<? super Try<? extends T>>, IO<Unit>> callback) {
    return new Async<>(callback);
  }

  static IO<Unit> unit() {
    return IOModule.UNIT;
  }

  static <T, R> IO<R> bracket(IO<? extends T> acquire, 
      Function1<? super T, ? extends IO<? extends R>> use, Function1<? super T, IO<Unit>> release) {
    return new Bracket<>(acquire, use, release);
  }

  static <T, R> IO<R> bracket(IO<? extends T> acquire, 
      Function1<? super T, ? extends IO<? extends R>> use, Consumer1<? super T> release) {
    return bracket(acquire, use, release.asFunction().andThen(IO::pure));
  }

  static <T extends AutoCloseable, R> IO<R> bracket(IO<? extends T> acquire, 
      Function1<? super T, ? extends IO<? extends R>> use) {
    return bracket(acquire, use, AutoCloseable::close);
  }

  static IO<Unit> sequence(Sequence<IO<?>> sequence) {
    return sequence.fold(unit(), IO::andThen).andThen(unit());
  }

  static <A> IO<Sequence<A>> traverse(Sequence<? extends IO<A>> sequence) {
    return sequence.foldLeft(pure(ImmutableList.empty()), 
        (IO<Sequence<A>> xs, IO<A> a) -> map2(xs, a, Sequence::append));
  }

  static <A, B, C> IO<C> map2(IO<? extends A> fa, IO<? extends B> fb,
                              Function2<? super A, ? super B, ? extends C> mapper) {
    return fb.ap(fa.map(mapper.curried()));
  }

  static <A, B> IO<Tuple2<A, B>> tuple(IO<? extends A> fa, IO<? extends B> fb) {
    return map2(fa, fb, Tuple::of);
  }

  final class Pure<T> implements SealedIO<T> {

    final T value;

    protected Pure(T value) {
      this.value = checkNonNull(value);
    }

    @Override
    public String toString() {
      return "Pure(" + value + ")";
    }
  }

  final class Failure<T> implements SealedIO<T>, Recoverable {

    final Throwable error;

    protected Failure(Throwable error) {
      this.error = checkNonNull(error);
    }

    @Override
    public String toString() {
      return "Failure(" + error + ")";
    }
  }

  final class FlatMapped<T, R> implements SealedIO<R> {

    final IO<? extends T> current;
    final Function1<? super T, ? extends IO<? extends R>> next;

    protected FlatMapped(IO<? extends T> current,
                         Function1<? super T, ? extends IO<? extends R>> next) {
      this.current = checkNonNull(current);
      this.next = checkNonNull(next);
    }

    @Override
    public String toString() {
      return "FlatMapped(" + current + ", ?)";
    }
  }

  final class Delay<T> implements SealedIO<T> {

    final Producer<? extends T> task;

    protected Delay(Producer<? extends T> task) {
      this.task = checkNonNull(task);
    }

    @Override
    public String toString() {
      return "SyncTask(?)";
    }
  }

  final class Async<T> implements SealedIO<T> {

    final Function1<Consumer1<? super Try<? extends T>>, IO<Unit>> callback;

    protected Async(Function1<Consumer1<? super Try<? extends T>>, IO<Unit>> callback) {
      this.callback = checkNonNull(callback);
    }

    @Override
    public String toString() {
      return "Async(?)";
    }
  }

  final class Suspend<T> implements SealedIO<T> {

    final Producer<? extends IO<? extends T>> lazy;

    protected Suspend(Producer<? extends IO<? extends T>> lazy) {
      this.lazy = checkNonNull(lazy);
    }

    @Override
    public String toString() {
      return "Suspend(?)";
    }
  }

  final class Recover<T> implements SealedIO<T> {

    final IO<T> current;
    final PartialFunction1<? super Throwable, ? extends IO<? extends T>> mapper;

    protected Recover(IO<T> current, PartialFunction1<? super Throwable, ? extends IO<? extends T>> mapper) {
      this.current = checkNonNull(current);
      this.mapper = checkNonNull(mapper);
    }

    @Override
    public String toString() {
      return "Recover(" + current + ", ?)";
    }
  }

  final class Bracket<T, R> implements SealedIO<R> {

    final IO<? extends T> acquire;
    final Function1<? super T, ? extends IO<? extends R>> use;
    final Function1<? super T, IO<Unit>> release;

    protected Bracket(IO<? extends T> acquire, Function1<? super T, ? extends IO<? extends R>> use, Function1<? super T, IO<Unit>> release) {
      this.acquire = checkNonNull(acquire);
      this.use = checkNonNull(use);
      this.release = checkNonNull(release);
    }

    @Override
    public String toString() {
      return "Bracket(" + acquire + ", ?, ?)";
    }
  }
}

interface IOModule {

  IO<Unit> UNIT = IO.pure(Unit.unit());
  
  static <T> Promise<T> runAsync(IO<T> current, IOConnection connection) {
    return runAsync(current, connection, new CallStack<>(), Promise.make());
  }

  @SuppressWarnings("unchecked")
  static <T, U, V> Promise<T> runAsync(IO<T> current, IOConnection connection, CallStack<T> stack, Promise<T> promise) {
    while (true) {
      try {
        current = unwrap(current, stack, identity());
        
        if (current instanceof IO.Pure) {
          return promise.succeeded(((IO.Pure<T>) current).value);
        }
        
        if (current instanceof IO.Async) {
          return executeAsync((IO.Async<T>) current, connection, promise);
        }
        
        if (current instanceof IO.FlatMapped) {
          stack.push();
          
          IO.FlatMapped<U, T> flatMapped = (FlatMapped<U, T>) current;
          IO<? extends U> source = unwrap(flatMapped.current, stack, u -> u.flatMap(flatMapped.next));
          
          if (source instanceof IO.Async) {
            Promise<U> nextPromise = Promise.make();
            
            nextPromise.then(u -> runAsync(flatMapped.next.andThen(IOOf::narrowK).apply(u), connection, stack, promise));
            
            executeAsync((IO.Async<U>) source, connection, nextPromise);
            
            return promise;
          }

          if (source instanceof IO.Pure) {
            IO.Pure<U> pure = (IO.Pure<U>) source;
            current = flatMapped.next.andThen(IOOf::narrowK).apply(pure.value);
          } else if (source instanceof IO.FlatMapped) {
            IO.FlatMapped<V, U> flatMapped2 = (FlatMapped<V, U>) source;
            current = flatMapped2.current.flatMap(a -> flatMapped2.next.apply(a).flatMap(flatMapped.next));
          }
        } else {
          stack.pop();
        }
      } catch (Throwable error) {
        Option<IO<T>> result = stack.tryHandle(error);
        
        if (result.isPresent()) {
          current = result.get();
        } else {
          return promise.failed(error);
        }
      }
    }
  }

  static <T, U> IO<T> unwrap(IO<T> current, CallStack<U> stack, Function1<IO<? extends T>, IO<? extends U>> next) {
    while (true) {
      if (current instanceof IO.Failure) {
        return stack.sneakyThrow(((IO.Failure<T>) current).error);
      } else if (current instanceof IO.Recover) {
        stack.add(((IO.Recover<T>) current).mapper.andThen(next));
        current = ((IO.Recover<T>) current).current;
      } else if (current instanceof IO.Suspend) {
        current = ((IO.Suspend<T>) current).lazy.andThen(IOOf::narrowK).get();
      } else if (current instanceof IO.Delay) {
        return IO.pure(((IO.Delay<T>) current).task.get());
      } else if (current instanceof IO.Pure) {
        return current;
      } else if (current instanceof IO.FlatMapped) {
        return current;
      } else if (current instanceof IO.Async) {
        return current;
      } else {
        throw new IllegalStateException();
      }
    }
  }

  static <T> Promise<T> executeAsync(IO.Async<T> current, IOConnection connection, Promise<T> promise) {
    if (connection.isCancellable() && !connection.updateState(StateIO::startingNow).isRunnable()) {
      return promise.cancel();
    }
    
    connection.setCancelToken(current.callback.apply(promise::tryComplete));
    
    promise.thenRun(() -> connection.setCancelToken(UNIT));
    
    if (connection.isCancellable() && connection.updateState(StateIO::notStartingNow).isCancelligNow()) {
      connection.cancelNow();
    }

    return promise;
  }

  static <T> IO<T> repeat(IO<T> self, IO<Unit> pause, int times) {
    return self.redeemWith(IO::raiseError, value -> {
      if (times > 0) {
        return pause.andThen(repeat(self, pause, times - 1));
      } else return IO.pure(value);
    });
  }

  static <T> IO<T> retry(IO<T> self, IO<Unit> pause, int maxRetries) {
    return self.redeemWith(error -> {
      if (maxRetries > 0) {
        return pause.andThen(retry(self, pause.repeat(), maxRetries - 1));
      } else return IO.raiseError(error);
    }, IO::pure);
  }
}

interface IOConnection {
  
  IOConnection UNCANCELLABLE = new IOConnection() {};
  
  default boolean isCancellable() { return false; }
  
  default void setCancelToken(IO<Unit> cancel) { }
  
  default void cancelNow() { }
  
  default void cancel() { }
  
  default StateIO updateState(Operator1<StateIO> update) { return StateIO.INITIAL; }
  
  static IOConnection cancellable() {
    return new IOConnection() {
      
      private IO<Unit> cancelToken;
      private final AtomicReference<StateIO> state = new AtomicReference<>(StateIO.INITIAL);
      
      @Override
      public boolean isCancellable() { return true; }
      
      @Override
      public void setCancelToken(IO<Unit> cancel) { this.cancelToken = checkNonNull(cancel); }
      
      @Override
      public void cancelNow() { cancelToken.runAsync(); }
      
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
    };
  }
}

final class StateIO {
  
  public static final StateIO INITIAL = new StateIO(false, false, false);
  public static final StateIO CANCELLED = new StateIO(true, false, false);
  
  private final boolean isCancelled;
  private final boolean cancelligNow;
  private final boolean startingNow;
  
  public StateIO(boolean isCancelled, boolean cancelligNow, boolean startingNow) {
    this.isCancelled = isCancelled;
    this.cancelligNow = cancelligNow;
    this.startingNow = startingNow;
  }
  
  public boolean isCancelled() {
    return isCancelled;
  }
  
  public boolean isCancelligNow() {
    return cancelligNow;
  }
  
  public boolean isStartingNow() {
    return startingNow;
  }
  
  public StateIO cancellingNow() {
    return new StateIO(isCancelled, true, startingNow);
  }
  
  public StateIO startingNow() {
    return new StateIO(isCancelled, cancelligNow, true);
  }
  
  public StateIO notStartingNow() {
    return new StateIO(isCancelled, cancelligNow, false);
  }
  
  public boolean isCancelable() {
    return !isCancelled && !cancelligNow && !startingNow;
  }
  
  public boolean isRunnable() {
    return !isCancelled && !cancelligNow;
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
  
  public void add(PartialFunction1<? super Throwable, ? extends IO<? extends T>> mapError) {
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
  private Deque<PartialFunction1<? super Throwable, ? extends IO<? extends T>>> recover = new LinkedList<>();

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
  
  public void add(PartialFunction1<? super Throwable, ? extends IO<? extends T>> mapError) {
    recover.addFirst(mapError);
  }

  public Option<IO<T>> tryHandle(Throwable error) {
    while (!recover.isEmpty()) {
      PartialFunction1<? super Throwable, ? extends IO<? extends T>> mapError = recover.removeFirst();
      if (mapError.isDefinedAt(error)) {
        return Option.some(mapError.andThen(IOOf::narrowK).apply(error));
      }
    }
    return Option.none();
  }
}