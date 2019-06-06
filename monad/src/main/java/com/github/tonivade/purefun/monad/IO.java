/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.monad;

import static com.github.tonivade.purefun.Function1.identity;
import static java.util.Objects.requireNonNull;

import java.util.concurrent.ExecutorService;

import com.github.tonivade.purefun.CheckedConsumer1;
import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.FlatMap1;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Recoverable;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Try;

@FunctionalInterface
public interface IO<T> extends FlatMap1<IO.µ, T>, Recoverable {

  IO<Unit> UNIT = pure(Unit.unit());

  final class µ implements Kind {}

  T unsafeRunSync();

  default Try<T> safeRunSync() {
    return Try.of(this::unsafeRunSync);
  }

  default Future<T> toFuture() {
    return toFuture(FutureModule.DEFAULT_EXECUTOR);
  }

  default Future<T> toFuture(ExecutorService executor) {
    throw new UnsupportedOperationException("not implemented");
  }

  default void safeRunAsync(Consumer1<Try<T>> callback) {
    toFuture().onComplete(callback);
  }

  default void safeRunAsync(ExecutorService executor, Consumer1<Try<T>> callback) {
    toFuture(executor).onComplete(callback);
  }

  @Override
  default <R> IO<R> map(Function1<T, R> map) {
    return flatMap(map.andThen(IO::pure));
  }

  @Override
  default <R> IO<R> flatMap(Function1<T, ? extends Higher1<IO.µ, R>> map) {
    return new FlatMapped<>(this, map);
  }

  default <R> IO<R> andThen(IO<R> after) {
    return flatMap(ignore -> after);
  }

  default IO<Try<T>> attemp() {
    return new Attemp<>(this);
  }

  default IO<Either<Throwable, T>> either() {
    return attemp().map(Try::toEither);
  }

  default <L, R> IO<Either<L, R>> either(Function1<Throwable, L> mapError, Function1<T, R> mapper) {
    return either().map(either -> either.bimap(mapError, mapper));
  }

  default <R> IO<R> redeem(Function1<Throwable, R> mapError, Function1<T, R> mapper) {
    return attemp().map(try_ -> try_.fold(mapError, mapper));
  }

  default <R> IO<R> redeemWith(Function1<Throwable, IO<R>> mapError, Function1<T, IO<R>> mapper) {
    return attemp().flatMap(try_ -> try_.fold(mapError, mapper));
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

  static <T> IO<T> pure(T value) {
    return new Pure<>(value);
  }

  static <T> IO<T> raiseError(Throwable error) {
    return new Failure<>(error);
  }

  static <T> IO<T> delay(Producer<T> lazy) {
    return suspend(lazy.andThen(IO::pure));
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
    return UNIT;
  }

  static <T, R> IO<R> bracket(IO<T> acquire, Function1<T, IO<R>> use, CheckedConsumer1<T> release) {
    return new Bracket<>(acquire, use, release);
  }

  static <T extends AutoCloseable, R> IO<R> bracket(IO<T> acquire, Function1<T, IO<R>> use) {
    return bracket(acquire, use, AutoCloseable::close);
  }

  static IO<Unit> sequence(Sequence<IO<?>> sequence) {
    return sequence.fold(unit(), IO::andThen).andThen(unit());
  }

  static <T> IO<T> narrowK(Higher1<IO.µ, T> hkt) {
    return (IO<T>) hkt;
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
    public Future<T> toFuture(ExecutorService executor) {
      return Future.success(executor, value);
    }

    @Override
    public String toString() {
      return "Pure(" + value + ")";
    }
  }

  final class FlatMapped<T, R> implements IO<R> {

    private final IO<T> current;
    private final Function1<T, ? extends Higher1<IO.µ, R>> next;

    private FlatMapped(IO<T> current, Function1<T, ? extends Higher1<IO.µ, R>> next) {
      this.current = requireNonNull(current);
      this.next = requireNonNull(next);
    }

    @Override
    public R unsafeRunSync() {
      return next.andThen(IO::narrowK).apply(current.unsafeRunSync()).unsafeRunSync();
    }
    
    @Override
    public Future<R> toFuture(ExecutorService executor) {
      return current.toFuture(executor).flatMap(next.andThen(IO::narrowK).andThen(io -> io.toFuture(executor)));
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
    public Future<T> toFuture(ExecutorService executor) {
      return Future.failure(executor, error);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> IO<R> map(Function1<T, R> map) {
      return (IO<R>) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> IO<R> flatMap(Function1<T, ? extends Higher1<IO.µ, R>> map) {
      return (IO<R>) this;
    }

    @Override
    public String toString() {
      return "Failure(" + error + ")";
    }
  }
  
  final class Task<T> implements IO<T> {

    private final Producer<T> task;
    
    public Task(Producer<T> task) {
      this.task = requireNonNull(task);
    }

    @Override
    public T unsafeRunSync() {
      return task.get();
    }
    
    @Override
    public Future<T> toFuture(ExecutorService executor) {
      return Future.from(task::get);
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
    public Future<T> toFuture(ExecutorService executor) {
      return Future.unit().andThen(lazy.andThen(io -> io.toFuture(executor)));
    }

    @Override
    public String toString() {
      return "Suspend(?)";
    }
  }

  final class Bracket<T, R> implements IO<R> {

    private final IO<T> acquire;
    private final Function1<T, IO<R>> use;
    private final CheckedConsumer1<T> release;

    private Bracket(IO<T> acquire, Function1<T, IO<R>> use, CheckedConsumer1<T> release) {
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
    public Future<R> toFuture(ExecutorService executor) {
      return acquire.toFuture(executor)
        .flatMap(value -> 
          Future.success(new IOResource<>(value, release))
            .flatMap(resource -> resource.apply(use).toFuture(executor)
                .onComplete(result -> resource.close())));
    }

    @Override
    public String toString() {
      return "Bracket(" + acquire + ", ?, ?)";
    }
  }

  final class Attemp<T> implements IO<Try<T>> {

    private final IO<T> current;

    private Attemp(IO<T> current) {
      this.current = requireNonNull(current);
    }

    @Override
    public Try<T> unsafeRunSync() {
      return Try.of(current::unsafeRunSync);
    }
    
    @Override
    public Future<Try<T>> toFuture(ExecutorService executor) {
      return current.toFuture().fold(Try::<T>failure, Try::success);
    }

    @Override
    public String toString() {
      return "Attemp(" + current + ")";
    }
  }
}

final class IOResource<T> implements AutoCloseable {
  final T resource;
  final CheckedConsumer1<T> release;

  IOResource(T resource, CheckedConsumer1<T> release) {
    this.resource = requireNonNull(resource);
    this.release = requireNonNull(release);
  }

  public <R> IO<R> apply(Function1<T, IO<R>> use) {
    return use.apply(resource);
  }

  @Override
  public void close() {
    release.unchecked().accept(resource);
  }
}