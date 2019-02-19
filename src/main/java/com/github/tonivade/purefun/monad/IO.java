/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.monad;

import static com.github.tonivade.purefun.Function1.identity;
import static com.github.tonivade.purefun.Nothing.nothing;
import static com.github.tonivade.purefun.Producer.cons;
import static java.util.Objects.requireNonNull;

import java.util.concurrent.ExecutorService;

import com.github.tonivade.purefun.CheckedConsumer1;
import com.github.tonivade.purefun.CheckedProducer;
import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.FlatMap1;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Nothing;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Recoverable;
import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.type.Future;
import com.github.tonivade.purefun.type.Try;

@FunctionalInterface
public interface IO<T> extends FlatMap1<IO.µ, T>, Recoverable {

  IO<Nothing> UNIT = pure(nothing());

  final class µ implements Kind {}

  T unsafeRunSync();

  default Future<T> toFuture() {
    return Future.run(this::unsafeRunSync);
  }

  default Future<T> toFuture(ExecutorService executor) {
    return Future.run(executor, this::unsafeRunSync);
  }

  default void unsafeRunAsync(Consumer1<Try<T>> callback) {
    toFuture().onComplete(callback);
  }

  default void unsafeRunAsync(ExecutorService executor, Consumer1<Try<T>> callback) {
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

  static IO<Nothing> exec(Runnable task) {
    return unit().flatMap(nothing -> { task.run(); return unit(); });
  }

  static <T> IO<T> of(Producer<T> producer) {
    return suspend(producer.andThen(IO::pure));
  }

  static IO<Nothing> unit() {
    return UNIT;
  }

  static <T, R> IO<R> bracket(IO<T> acquire, Function1<T, IO<R>> use, CheckedConsumer1<T> release) {
    return new Bracket<>(acquire, use, release);
  }

  static <T extends AutoCloseable, R> IO<R> bracket(IO<T> acquire, Function1<T, IO<R>> use) {
    return bracket(acquire, use, AutoCloseable::close);
  }

  static IO<Nothing> sequence(Sequence<IO<?>> sequence) {
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
    public String toString() {
      return "FlatMapped(" + current + ", ?)";
    }
  }

  final class Failure<T> implements IO<T> {

    private final Throwable error;

    private Failure(Throwable error) {
      this.error = requireNonNull(error);
    }

    @Override
    public T unsafeRunSync() {
      return toCheckedProducer().unchecked().get();
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

    private CheckedProducer<T> toCheckedProducer() {
      return CheckedProducer.failure(cons(error));
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
    public String toString() {
      return "Attemp(" + current + ")";
    }
  }
}

final class IOResource<T> implements AutoCloseable {
  final T resource;
  final CheckedConsumer1<T> release;

  IOResource(T resource, CheckedConsumer1<T> release) {
    this.resource = resource;
    this.release = release;
  }

  public <R> IO<R> apply(Function1<T, IO<R>> use) {
    return use.apply(resource);
  }

  @Override
  public void close() {
    release.unchecked().accept(resource);
  }
}