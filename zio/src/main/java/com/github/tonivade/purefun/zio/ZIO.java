/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.zio;

import static com.github.tonivade.purefun.Function1.cons;
import static com.github.tonivade.purefun.Function1.identity;
import static java.util.Objects.requireNonNull;

import java.util.concurrent.ExecutorService;

import com.github.tonivade.purefun.CheckedFunction1;
import com.github.tonivade.purefun.CheckedProducer;
import com.github.tonivade.purefun.CheckedRunnable;
import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.FlatMap3;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Function2;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Higher2;
import com.github.tonivade.purefun.Higher3;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Nothing;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.concurrent.Future;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Try;

public interface ZIO<R, E, A> extends FlatMap3<ZIO.µ, R, E, A> {

  final class µ implements Kind {}

  Either<E, A> provide(R env);

  Future<Either<E, A>> toFuture(ExecutorService executor, R env);

  default Future<Either<E, A>> toFuture(R env) {
    return toFuture(Future.DEFAULT_EXECUTOR, env);
  }

  default void provideAsync(ExecutorService executor, R env, Consumer1<Try<Either<E, A>>> callback) {
    toFuture(executor, env).onComplete(callback);
  }

  default void provideAsync(R env, Consumer1<Try<Either<E, A>>> callback) {
    provideAsync(Future.DEFAULT_EXECUTOR, env, callback);
  }

  @Override
  default <B> ZIO<R, E, B> map(Function1<A, B> map) {
    return new FlatMapped<>(this, ZIO::raiseError, map.andThen(ZIO::pure));
  }

  @Override
  default <B> ZIO<R, E, B> flatMap(Function1<A, ? extends Higher3<ZIO.µ, R, E, B>> map) {
    return new FlatMapped<>(this, ZIO::raiseError, map.andThen(ZIO::narrowK));
  }

  @SuppressWarnings("unchecked")
  default <B> ZIO<R, E, B> flatten() {
    try {
      return ((ZIO<R, E, ZIO<R, E, B>>) this).flatMap(identity());
    } catch (ClassCastException e) {
      throw new UnsupportedOperationException("cannot be flattened");
    }
  }

  default ZIO<R, A, E> swap() {
    return new Swap<>(this);
  }

  default <B> ZIO<R, B, A> mapError(Function1<E, B> map) {
    return new FlatMapped<>(this, map.andThen(ZIO::raiseError), ZIO::pure);
  }

  default <F> ZIO<R, F, A> flatMapError(Function1<E, ZIO<R, F, A>> map) {
    return new FlatMapped<>(this, map, ZIO::pure);
  }

  default <B, F> ZIO<R, F, B> bimap(Function1<E, F> mapError, Function1<A, B> map) {
    return new FlatMapped<>(this, mapError.andThen(ZIO::raiseError), map.andThen(ZIO::pure));
  }

  default <B> ZIO<R, E, B> andThen(ZIO<R, E, B> next) {
    return flatMap(ignore -> next);
  }

  default <B, F> ZIO<R, F, B> foldM(Function1<E, ZIO<R, F, B>> mapError, Function1<A, ZIO<R, F, B>> map) {
    return new FoldM<>(this, mapError, map);
  }

  default <B> ZIO<R, Nothing, B> fold(Function1<E, B> mapError, Function1<A, B> map) {
    return foldM(mapError.andThen(ZIO::pure), map.andThen(ZIO::pure));
  }

  default ZIO<R, E, A> orElse(Producer<ZIO<R, E, A>> other) {
    return foldM(other.asFunction(), cons(this));
  }

  ZIOModule getModule();

  static <R, E, A> ZIO<R, E, A> accessM(Function1<R, ZIO<R, E, A>> map) {
    return new AccessM<>(map);
  }

  static <R, A> ZIO<R, Nothing, A> access(Function1<R, A> map) {
    return accessM(map.andThen(ZIO::pure));
  }

  static <R> ZIO<R, Nothing, R> env() {
    return access(identity());
  }

  static <R, E, A, B, C> ZIO<R, E, C> map2(ZIO<R, E, A> za, ZIO<R, E, B> zb, Function2<A, B, C> mapper) {
    return za.flatMap(a -> zb.map(b -> mapper.curried().apply(a).apply(b)));
  }

  static <R, E, A> ZIO<R, E, A> absorb(ZIO<R, E, Either<E, A>> value) {
    return value.flatMap(either -> either.fold(ZIO::raiseError, ZIO::pure));
  }

  static <R, A, B> Function1<A, ZIO<R, Throwable, B>> lift(CheckedFunction1<A, B> function) {
    return value -> from(() -> function.apply(value));
  }

  static <R, E, A> ZIO<R, E, A> from(Producer<Either<E, A>> task) {
    return new Task<>(task);
  }

  static <R, A> ZIO<R, Throwable, A> from(CheckedProducer<A> task) {
    return new Attemp<>(task);
  }

  static <R> ZIO<R, Throwable, Unit> exec(CheckedRunnable task) {
    return new Attemp<>(task.asProducer());
  }

  static <R, E, A> ZIO<R, E, A> pure(A value) {
    return new Pure<>(value);
  }

  static <R, E, A> ZIO<R, E, A> pure(Producer<A> task) {
    return new Task<>(task.andThen(Either::right));
  }

  static <R, E, A> ZIO<R, E, A> raiseError(E error) {
    return new Failure<>(error);
  }

  @SuppressWarnings("unchecked")
  static <R, E> ZIO<R, E, Unit> unit() {
    return (ZIO<R, E, Unit>) ZIOModule.UNIT;
  }

  static <R, E, A> ZIO<R, E, A> narrowK(Higher3<ZIO.µ, R, E, A> hkt) {
    return (ZIO<R, E, A>) hkt;
  }

  static <R, E, A> ZIO<R, E, A> narrowK(Higher2<Higher1<ZIO.µ, R>, E, A> hkt) {
    return (ZIO<R, E, A>) hkt;
  }

  @SuppressWarnings("unchecked")
  static <R, E, A> ZIO<R, E, A> narrowK(Higher1<Higher1<Higher1<ZIO.µ, R>, E>, A> hkt) {
    return (ZIO<R, E, A>) hkt;
  }

  final class Pure<R, E, A> implements ZIO<R, E, A> {

    private A value;

    private Pure(A value) {
      this.value = requireNonNull(value);
    }

    @Override
    public Either<E, A> provide(R env) {
      return Either.right(value);
    }

    @Override
    public Future<Either<E, A>> toFuture(ExecutorService executor, R env) {
      return Future.success(executor, Either.right(value));
    }

    @Override
    public ZIOModule getModule() {
      throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
      return "Pure(" + value + ")";
    }
  }

  final class Failure<R, E, A> implements ZIO<R, E, A> {

    private E error;

    private Failure(E error) {
      this.error = requireNonNull(error);
    }

    @Override
    public Either<E, A> provide(R env) {
      return Either.left(error);
    }

    @Override
    public Future<Either<E, A>> toFuture(ExecutorService executor, R env) {
      return Future.success(executor, Either.left(error));
    }

    @Override
    public ZIOModule getModule() {
      throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
      return "Failure(" + error + ")";
    }
  }

  final class FlatMapped<R, E, A, F, B> implements ZIO<R, F, B> {

    private ZIO<R, E, A> current;
    private Function1<E, ZIO<R, F, B>> nextError;
    private Function1<A, ZIO<R, F, B>> next;

    private FlatMapped(ZIO<R, E, A> current,
                       Function1<E, ZIO<R, F, B>> nextError,
                       Function1<A, ZIO<R, F, B>> next) {
      this.current = requireNonNull(current);
      this.nextError = requireNonNull(nextError);
      this.next = requireNonNull(next);
    }

    @Override
    public Either<F, B> provide(R env) {
      Either<ZIO<R, F, B>, ZIO<R, F, B>> bimap = current.provide(env).bimap(nextError, next);
      return bimap.fold(identity(), identity()).provide(env);
    }

    @Override
    public Future<Either<F, B>> toFuture(ExecutorService executor, R env) {
      Future<Either<E, A>> future = current.toFuture(executor, env);
      Future<Either<ZIO<R, F, B>, ZIO<R, F, B>>> flatMap =
          future.flatMap(either -> Future.success(executor, either.bimap(nextError, next)));
      return flatMap.flatMap(either -> either.fold(identity(), identity()).toFuture(executor, env));
    }

    @Override
    public ZIOModule getModule() {
      throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
      return "FlatMapped(" + current + ", ?, ?)";
    }
  }

  final class Task<R, E, A> implements ZIO<R, E, A> {

    private Producer<Either<E, A>> task;

    private Task(Producer<Either<E, A>> task) {
      this.task = requireNonNull(task);
    }

    @Override
    public Either<E, A> provide(R env) {
      return task.get();
    }

    @Override
    public Future<Either<E, A>> toFuture(ExecutorService executor, R env) {
      return Future.run(executor, task::get);
    }

    @Override
    public ZIOModule getModule() {
      throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
      return "Task(?)";
    }
  }

  final class Swap<R, E, A> implements ZIO<R, A, E> {

    private ZIO<R, E, A> current;

    private Swap(ZIO<R, E, A> current) {
      this.current = requireNonNull(current);
    }

    @Override
    public Either<A, E> provide(R env) {
      return current.provide(env).swap();
    }

    @Override
    public Future<Either<A, E>> toFuture(ExecutorService executor, R env) {
      return current.toFuture(executor, env).map(Either::swap);
    }

    @Override
    public ZIOModule getModule() {
      throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
      return "Swap(" + current + ")";
    }
  }

  final class Attemp<R, A> implements ZIO<R, Throwable, A> {

    private final CheckedProducer<A> current;

    private Attemp(CheckedProducer<A> current) {
      this.current = requireNonNull(current);
    }

    @Override
    public Either<Throwable, A> provide(R env) {
      return Try.of(current).toEither();
    }

    @Override
    public Future<Either<Throwable, A>> toFuture(ExecutorService executor, R env) {
      return Future.run(() -> Try.of(current).toEither());
    }

    @Override
    public ZIOModule getModule() {
      throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
      return "Attemp(" + current + ")";
    }
  }

  final class AccessM<R, E, A> implements ZIO<R, E, A> {

    private Function1<R, ZIO<R, E, A>> function;

    private AccessM(Function1<R, ZIO<R, E, A>> function) {
      this.function = requireNonNull(function);
    }

    @Override
    public Either<E, A> provide(R env) {
      return function.apply(env).provide(env);
    }

    @Override
    public Future<Either<E, A>> toFuture(ExecutorService executor, R env) {
      return Future.run(executor, () -> function.apply(env))
          .flatMap(zio -> zio.toFuture(executor, env));
    }

    @Override
    public ZIOModule getModule() {
      throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
      return "AccessM(?)";
    }
  }

  final class FoldM<R, E, A, F, B> implements ZIO<R, F, B> {

    private ZIO<R, E, A> current;
    private Function1<E, ZIO<R, F, B>> nextError;
    private Function1<A, ZIO<R, F, B>> next;

    private FoldM(ZIO<R, E, A> current, Function1<E, ZIO<R, F, B>> nextError, Function1<A, ZIO<R, F, B>> next) {
      this.current = requireNonNull(current);
      this.nextError = requireNonNull(nextError);
      this.next = requireNonNull(next);
    }

    @Override
    public Either<F, B> provide(R env) {
      return current.provide(env).fold(nextError, next).provide(env);
    }

    @Override
    public Future<Either<F, B>> toFuture(ExecutorService executor, R env) {
      Future<Either<E, A>> future = current.toFuture(executor, env);
      Future<ZIO<R, F, B>> map = future.map(either -> either.fold(nextError, next));
      return map.flatMap(zio -> zio.toFuture(executor, env));
    }

    @Override
    public ZIOModule getModule() {
      throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
      return "FoldM(" + current + ", ?, ?)";
    }
  }
}

interface ZIOModule {

  ZIO<?, ?, Unit> UNIT = ZIO.pure(Unit.unit());
}