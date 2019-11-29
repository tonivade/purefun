/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.monad;

import static com.github.tonivade.purefun.Function1.identity;
import static java.util.Objects.requireNonNull;

import java.util.concurrent.Executor;

import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Recoverable;
import com.github.tonivade.purefun.Sealed;
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
    return new FlatMapped<>(this, map);
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

  static IO<Unit> exec(Runnable task) {
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

    private Pure(T value) {
      this.value = requireNonNull(value);
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

    private final IO<T> current;
    private final Function1<T, IO<R>> next;

    private FlatMapped(IO<T> current, Function1<T, IO<R>> next) {
      this.current = requireNonNull(current);
      this.next = requireNonNull(next);
    }

    @Override
    public R unsafeRunSync() {
      return next.apply(current.unsafeRunSync()).unsafeRunSync();
    }

    @Override
    public <F extends Kind> Higher1<F, R> foldMap(MonadDefer<F> monad) {
      return monad.flatMap(current.foldMap(monad), next.andThen(io -> io.foldMap(monad)));
    }

    @Override
    public IOModule getModule() {
      throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
      return "FlatMapped(" + current + ", ?)";
    }
  }

  final class Failure<T> implements IO<T>, Recoverable {

    private final Throwable error;

    private Failure(Throwable error) {
      this.error = requireNonNull(error);
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

    private Task(Producer<T> task) {
      this.task = requireNonNull(task);
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

    private Suspend(Producer<IO<T>> lazy) {
      this.lazy = requireNonNull(lazy);
    }

    @Override
    public T unsafeRunSync() {
      return lazy.get().unsafeRunSync();
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
  }

  final class Bracket<T, R> implements IO<R> {

    private final IO<T> acquire;
    private final Function1<T, IO<R>> use;
    private final Consumer1<T> release;

    private Bracket(IO<T> acquire, Function1<T, IO<R>> use, Consumer1<T> release) {
      this.acquire = requireNonNull(acquire);
      this.use = requireNonNull(use);
      this.release = requireNonNull(release);
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

    private Attempt(IO<T> current) {
      this.current = requireNonNull(current);
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
}

final class IOResource<T> implements AutoCloseable {

  private final T resource;
  private final Consumer1<T> release;

  IOResource(T resource, Consumer1<T> release) {
    this.resource = requireNonNull(resource);
    this.release = requireNonNull(release);
  }

  public <R> IO<R> apply(Function1<T, IO<R>> use) {
    return use.apply(resource);
  }

  @Override
  public void close() {
    release.accept(resource);
  }
}