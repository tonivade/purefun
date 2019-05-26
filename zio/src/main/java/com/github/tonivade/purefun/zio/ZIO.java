/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.zio;

import static com.github.tonivade.purefun.Function1.cons;
import static com.github.tonivade.purefun.Function1.identity;
import static com.github.tonivade.purefun.Nothing.nothing;
import static com.github.tonivade.purefun.zio.ZIOModule.flatMapValue;
import static com.github.tonivade.purefun.zio.ZIOModule.mapValue;

import com.github.tonivade.purefun.CheckedFunction1;
import com.github.tonivade.purefun.CheckedProducer;
import com.github.tonivade.purefun.CheckedRunnable;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Function2;
import com.github.tonivade.purefun.Nothing;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Try;

@FunctionalInterface
public interface ZIO<R, E, A> {

  ZIO<?, ?, Nothing> UNIT = pure(nothing());

  Either<E, A> provide(R env);

  default <B> ZIO<R, E, B> map(Function1<A, B> map) {
    return mapValue(this, value -> value.map(map));
  }

  default <B> ZIO<R, E, B> flatMap(Function1<A, ZIO<R, E, B>> map) {
    return flatMapValue(this, value -> value.map(map).fold(ZIO::raiseError, identity()));
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
    return mapValue(this, Either<E, A>::swap);
  }

  default <B> ZIO<R, B, A> mapError(Function1<E, B> map) {
    return mapValue(this, value -> value.mapLeft(map));
  }

  default <F> ZIO<R, F, A> flatMapError(Function1<E, ZIO<R, F, A>> map) {
    return flatMapValue(this, value -> value.mapLeft(map).fold(identity(), ZIO::pure));
  }

  default <B, F> ZIO<R, F, B> bimap(Function1<E, F> mapError, Function1<A, B> map) {
    return mapValue(this, value -> value.bimap(mapError, map));
  }

  default <B> ZIO<R, E, B> andThen(Producer<ZIO<R, E, B>> next) {
    return flatMap(ignore -> next.get());
  }

  default <B, F> ZIO<R, F, B> foldM(Function1<E, ZIO<R, F, B>> mapError, Function1<A, ZIO<R, F, B>> map) {
    return env -> provide(env).fold(mapError, map).provide(env);
  }

  default <B> ZIO<R, Nothing, B> fold(Function1<E, B> mapError, Function1<A, B> map) {
    return foldM(mapError.andThen(ZIO::pure), map.andThen(ZIO::pure));
  }

  default ZIO<R, E, A> orElse(Producer<ZIO<R, E, A>> other) {
    return foldM(other.asFunction(), cons(this));
  }

  static <R, E, A> ZIO<R, E, A> accessM(Function1<R, ZIO<R, E, A>> map) {
    return env -> map.apply(env).provide(env);
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
    return mapValue(value, Either::flatten);
  }

  static <R, A, B> Function1<A, ZIO<R, Throwable, B>> lift(CheckedFunction1<A, B> function) {
    return value -> from(() -> function.apply(value));
  }

  static <R, E, A> ZIO<R, E, A> from(Producer<Either<E, A>> task) {
    return env -> task.get();
  }

  static <R, A> ZIO<R, Throwable, A> from(CheckedProducer<A> task) {
    return env -> Try.of(task).toEither();
  }

  static <R> ZIO<R, Throwable, Nothing> exec(CheckedRunnable task) {
    return from(task.asProducer());
  }

  static <R, E, A> ZIO<R, E, A> pure(A value) {
    return env -> Either.right(value);
  }

  static <R, E, A> ZIO<R, E, A> pure(Producer<A> value) {
    return env -> Either.right(value.get());
  }

  static <R, E, A> ZIO<R, E, A> raiseError(E error) {
    return env -> Either.left(error);
  }

  @SuppressWarnings("unchecked")
  static <R, E> ZIO<R, E, Nothing> unit() {
    return (ZIO<R, E, Nothing>) UNIT;
  }
}

interface ZIOModule {

  static <R, E, F, A, B> ZIO<R, F, B> mapValue(ZIO<R, E, A> self, Function1<Either<E, A>, Either<F, B>> map) {
    return env -> map.apply(self.provide(env));
  }

  static <R, E, F, A, B> ZIO<R, F, B> flatMapValue(ZIO<R, E, A> self, Function1<Either<E, A>, ZIO<R, F, B>> map) {
    return env -> map.apply(self.provide(env)).provide(env);
  }
}