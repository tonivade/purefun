/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.monad;

import static com.github.tonivade.purefun.Function1.identity;
import static com.github.tonivade.purefun.Precondition.checkNonNull;

import java.time.Duration;
import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.Executor;

import com.github.tonivade.purefun.CheckedRunnable;
import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Recoverable;
import com.github.tonivade.purefun.Sealed;
import com.github.tonivade.purefun.Tuple;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.concurrent.Future;
import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Try;
import com.github.tonivade.purefun.typeclasses.MonadDefer;

@Sealed
@HigherKind
public interface IO<T> extends Recoverable {

  T unsafeRunSync();

  default Try<T> safeRunSync() {
    return Try.of(this::unsafeRunSync);
  }

  default Future<T> toFuture() {
    return toFuture(Future.DEFAULT_EXECUTOR);
  }

  default Future<T> toFuture(Executor executor) {
    return Future.async(executor, this::unsafeRunSync);
  }

  default void safeRunAsync(Consumer1<Try<T>> callback) {
    safeRunAsync(Future.DEFAULT_EXECUTOR, callback);
  }

  default void safeRunAsync(Executor executor, Consumer1<Try<T>> callback) {
    toFuture(executor).onComplete(callback);
  }

  <F extends Kind> Higher1<F, T> foldMap(MonadDefer<F> monad);

  default <R> IO<R> map(Function1<T, R> map) {
    return flatMap(map.andThen(IO::pure));
  }

  default <R> IO<R> flatMap(Function1<T, IO<R>> map) {
    return new FlatMapped<>(Producer.cons(this), map);
  }

  default <R> IO<R> andThen(IO<R> after) {
    return flatMap(ignore -> after);
  }

  default IO<Try<T>> attempt() {
    return new Attempt<>(this);
  }

  default IO<Either<Throwable, T>> either() {
    return attempt().map(Try::toEither);
  }

  default <L, R> IO<Either<L, R>> either(Function1<Throwable, L> mapError, Function1<T, R> mapper) {
    return either().map(either -> either.bimap(mapError, mapper));
  }

  default <R> IO<R> redeem(Function1<Throwable, R> mapError, Function1<T, R> mapper) {
    return attempt().map(try_ -> try_.fold(mapError, mapper));
  }

  default <R> IO<R> redeemWith(Function1<Throwable, IO<R>> mapError, Function1<T, IO<R>> mapper) {
    return attempt().flatMap(try_ -> try_.fold(mapError, mapper));
  }

  default IO<T> recover(Function1<Throwable, T> mapError) {
    return redeem(mapError, identity());
  }

  @SuppressWarnings("unchecked")
  default <X extends Throwable> IO<T> recoverWith(Class<X> type, Function1<X, T> function) {
    return recover(cause -> {
      if (type.isAssignableFrom(cause.getClass())) {
        return function.apply((X) cause);
      }
      return sneakyThrow(cause);
    });
  }

  default IO<Tuple2<Duration, T>> timed() {
    return IO.task(() -> {
      long start = System.nanoTime();
      T result = unsafeRunSync();
      return Tuple.of(Duration.ofNanos(System.nanoTime() - start), result);
    });
  }

  default IO<T> repeat() {
    return repeat(1);
  }

  default IO<T> repeat(int times) {
    return IOModule.repeat(this, unit(), times);
  }

  default IO<T> repeat(Duration delay) {
    return repeat(delay, 1);
  }

  default IO<T> repeat(Duration delay, int times) {
    return IOModule.repeat(this, sleep(delay), times);
  }

  default IO<T> retry() {
    return retry(1);
  }

  default IO<T> retry(int maxRetries) {
    return IOModule.retry(this, unit(), maxRetries);
  }

  default IO<T> retry(Duration delay) {
    return retry(delay, 1);
  }

  default IO<T> retry(Duration delay, int maxRetries) {
    return IOModule.retry(this, sleep(delay), maxRetries);
  }

  IOModule getModule();

  static <T> IO<T> pure(T value) {
    return new Pure<>(value);
  }

  static <T> IO<T> raiseError(Throwable error) {
    return new Failure<>(error);
  }

  static <T> IO<T> delay(Producer<T> lazy) {
    return suspend(lazy.map(IO::pure));
  }

  static <T> IO<T> suspend(Producer<IO<T>> lazy) {
    return new Suspend<>(lazy);
  }

  static <T, R> Function1<T, IO<R>> lift(Function1<T, R> task) {
    return task.andThen(IO::pure);
  }

  static IO<Unit> sleep(Duration duration) {
    return new Sleep(duration);
  }

  static IO<Unit> exec(CheckedRunnable task) {
    return task(() -> { task.run(); return Unit.unit(); });
  }

  static <T> IO<T> task(Producer<T> producer) {
    return new Task<>(producer);
  }

  static IO<Unit> unit() {
    return IOModule.UNIT;
  }

  static <T, R> IO<R> bracket(IO<T> acquire, Function1<T, IO<R>> use, Consumer1<T> release) {
    return new Bracket<>(acquire, use, release);
  }

  static <T extends AutoCloseable, R> IO<R> bracket(IO<T> acquire, Function1<T, IO<R>> use) {
    return bracket(acquire, use, AutoCloseable::close);
  }

  static IO<Unit> sequence(Sequence<IO<?>> sequence) {
    return sequence.fold(unit(), IO::andThen).andThen(unit());
  }

  final class Pure<T> implements IO<T> {

    private final T value;

    protected Pure(T value) {
      this.value = checkNonNull(value);
    }

    @Override
    public T unsafeRunSync() {
      return value;
    }

    @Override
    public <F extends Kind> Higher1<F, T> foldMap(MonadDefer<F> monad) {
      return monad.pure(value);
    }

    @Override
    public IOModule getModule() {
      throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
      return "Pure(" + value + ")";
    }
  }

  final class FlatMapped<T, R> implements IO<R> {

    private final Producer<IO<T>> current;
    private final Function1<T, IO<R>> next;

    protected FlatMapped(Producer<IO<T>> current, Function1<T, IO<R>> next) {
      this.current = checkNonNull(current);
      this.next = checkNonNull(next);
    }

    @Override
    public R unsafeRunSync() {
      return IOModule.evaluate(this);
    }

    @Override
    public <F extends Kind> Higher1<F, R> foldMap(MonadDefer<F> monad) {
      return monad.flatMap(current.get().foldMap(monad), next.andThen(io -> io.foldMap(monad)));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R1> IO<R1> flatMap(Function1<R, IO<R1>> map) {
      return new FlatMapped<>(() -> (IO<R>) start(), r -> new FlatMapped<>(() -> run((T) r), map::apply));
    }

    @Override
    public IOModule getModule() {
      throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
      return "FlatMapped(" + current + ", ?)";
    }

    protected IO<T> start() {
      return current.get();
    }

    protected IO<R> run(T value) {
      return next.apply(value);
    }
  }

  final class Failure<T> implements IO<T>, Recoverable {

    private final Throwable error;

    protected Failure(Throwable error) {
      this.error = checkNonNull(error);
    }

    @Override
    public T unsafeRunSync() {
      return sneakyThrow(error);
    }

    @Override
    public <F extends Kind> Higher1<F, T> foldMap(MonadDefer<F> monad) {
      return monad.raiseError(error);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> IO<R> map(Function1<T, R> map) {
      return (IO<R>) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> IO<R> flatMap(Function1<T, IO<R>> map) {
      return (IO<R>) this;
    }

    @Override
    public IOModule getModule() {
      throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
      return "Failure(" + error + ")";
    }
  }

  final class Task<T> implements IO<T> {

    private final Producer<T> task;

    protected Task(Producer<T> task) {
      this.task = checkNonNull(task);
    }

    @Override
    public T unsafeRunSync() {
      return task.get();
    }

    @Override
    public <F extends Kind> Higher1<F, T> foldMap(MonadDefer<F> monad) {
      return monad.later(task);
    }

    @Override
    public IOModule getModule() {
      throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
      return "Task(?)";
    }
  }

  final class Suspend<T> implements IO<T> {

    private final Producer<IO<T>> lazy;

    protected Suspend(Producer<IO<T>> lazy) {
      this.lazy = checkNonNull(lazy);
    }

    @Override
    public T unsafeRunSync() {
      return IOModule.collapse(this).unsafeRunSync();
    }

    @Override
    public <R> IO<R> flatMap(Function1<T, IO<R>> map) {
      return new FlatMapped<>(lazy::get, map::apply);
    }

    @Override
    public <F extends Kind> Higher1<F, T> foldMap(MonadDefer<F> monad) {
      return monad.defer(() -> lazy.get().foldMap(monad));
    }

    @Override
    public IOModule getModule() {
      throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
      return "Suspend(?)";
    }

    protected IO<T> next() {
      return lazy.get();
    }
  }

  final class Sleep implements IO<Unit>, Recoverable {

    private final Duration duration;

    public Sleep(Duration duration) {
      this.duration = checkNonNull(duration);
    }

    @Override
    public Unit unsafeRunSync() {
      try {
        Thread.sleep(duration.toMillis());
        return Unit.unit();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return sneakyThrow(e);
      }
    }

    @Override
    public <F extends Kind> Higher1<F, Unit> foldMap(MonadDefer<F> monad) {
      return monad.sleep(duration);
    }

    @Override
    public IOModule getModule() {
      throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
      return "Sleep(" + duration + ')';
    }
  }

  final class Bracket<T, R> implements IO<R> {

    private final IO<T> acquire;
    private final Function1<T, IO<R>> use;
    private final Consumer1<T> release;

    protected Bracket(IO<T> acquire, Function1<T, IO<R>> use, Consumer1<T> release) {
      this.acquire = checkNonNull(acquire);
      this.use = checkNonNull(use);
      this.release = checkNonNull(release);
    }

    @Override
    public R unsafeRunSync() {
      try (IOResource<T> resource = new IOResource<>(acquire.unsafeRunSync(), release)) {
        return resource.apply(use).unsafeRunSync();
      }
    }

    @Override
    public <F extends Kind> Higher1<F, R> foldMap(MonadDefer<F> monad) {
      return monad.bracket(acquire.foldMap(monad), use.andThen(io -> io.foldMap(monad)), release);
    }

    @Override
    public IOModule getModule() {
      throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
      return "Bracket(" + acquire + ", ?, ?)";
    }
  }

  final class Attempt<T> implements IO<Try<T>> {

    private final IO<T> current;

    protected Attempt(IO<T> current) {
      this.current = checkNonNull(current);
    }

    @Override
    public Try<T> unsafeRunSync() {
      return Try.of(current::unsafeRunSync);
    }

    @Override
    public <F extends Kind> Higher1<F, Try<T>> foldMap(MonadDefer<F> monad) {
      return monad.map(monad.attempt(current.foldMap(monad)), Try::fromEither);
    }

    @Override
    public IOModule getModule() {
      throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
      return "Attempt(" + current + ")";
    }
  }
}

interface IOModule {

  IO<Unit> UNIT = IO.pure(Unit.unit());

  static <A, X> IO<A> collapse(IO<A> self) {
    IO<A> current = self;
    while (true) {
      if (current instanceof IO.Suspend) {
        IO.Suspend<A> suspend = (IO.Suspend<A>) current;
        current = suspend.next();
      } else if (current instanceof IO.FlatMapped) {
        IO.FlatMapped<X, A> flatMapped = (IO.FlatMapped<X, A>) current;
        return new IO.FlatMapped<>(flatMapped::start, a -> collapse(flatMapped.run(a)));
      } else break;
    }
    return current;
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  static <A> A evaluate(IO<A> self) {
    Deque<Function1<Object, IO>> stack = new LinkedList<>();
    IO<A> current = self;
    while (true) {
      if (current instanceof IO.FlatMapped) {
        IO.FlatMapped currentFlatMapped = (IO.FlatMapped) current;
        IO<A> next = currentFlatMapped.start();
        if (next instanceof IO.FlatMapped) {
          IO.FlatMapped nextFlatMapped = (IO.FlatMapped) next;
          current = nextFlatMapped.start();
          stack.push(currentFlatMapped::run);
          stack.push(nextFlatMapped::run);
        } else {
          current = (IO<A>) currentFlatMapped.run(next.unsafeRunSync());
        }
      } else if (!stack.isEmpty()) {
        current = (IO<A>) stack.pop().apply(current.unsafeRunSync());
      } else break;
    }
    return current.unsafeRunSync();
  }

  static <T> IO<T> repeat(IO<T> self, IO<Unit> pause, int times) {
    return self.redeemWith(IO::raiseError, value -> {
      if (times > 0)
        return pause.andThen(repeat(self, pause, times - 1));
      else
        return IO.pure(value);
    });
  }

  static <T> IO<T> retry(IO<T> self, IO<Unit> pause, int maxRetries) {
    return self.redeemWith(error -> {
      if (maxRetries > 0)
        return pause.andThen(retry(self, pause.repeat(), maxRetries - 1));
      else
        return IO.raiseError(error);
    }, IO::pure);
  }
}

final class IOResource<T> implements AutoCloseable {

  private final T resource;
  private final Consumer1<T> release;

  IOResource(T resource, Consumer1<T> release) {
    this.resource = checkNonNull(resource);
    this.release = checkNonNull(release);
  }

  public <R> IO<R> apply(Function1<T, IO<R>> use) {
    return use.apply(resource);
  }

  @Override
  public void close() {
    release.accept(resource);
  }
}