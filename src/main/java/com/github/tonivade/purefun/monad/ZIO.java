/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.monad;

import static com.github.tonivade.purefun.Function1.identity;

import com.github.tonivade.purefun.CheckedFunction1;
import com.github.tonivade.purefun.CheckedProducer;
import com.github.tonivade.purefun.CheckedRunnable;
import com.github.tonivade.purefun.FlatMap3;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Higher2;
import com.github.tonivade.purefun.Higher3;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Nothing;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Try;

@FunctionalInterface
public interface ZIO<R, E, A> extends FlatMap3<ZIO.µ, R, E, A> {

  final class µ implements Kind {}

  IO<Either<E, A>> provide(R env);

  default Either<E, A> run(R env) {
    return provide(env).unsafeRunSync();
  }

  @Override
  default <B> ZIO<R, E, B> map(Function1<A, B> map) {
    return env -> provide(env).map(either -> either.map(map));
  }

  @Override
  default <B> ZIO<R, E, B> flatMap(Function1<A, ? extends Higher3<ZIO.µ, R, E, B>> map) {
    return env -> provide(env).flatMap(value -> value.map(map.andThen(ZIO::narrowK)).fold(ZIO::raiseError, identity()).provide(env));
  }

  default <B> ZIO<R, E, B> andThen(Producer<? extends Higher3<ZIO.µ, R, E, B>> next) {
    return flatMap(ignore -> next.get());
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

  static <R, A, B> Function1<A, ZIO<R, Throwable, B>> lift(CheckedFunction1<A, B> function) {
    return value -> from(() ->function.apply(value));
  }

  static <R, A> ZIO<R, Throwable, A> from(CheckedProducer<A> task) {
    return env -> IO.of(() -> Try.of(task::get).toEither());
  }

  static <R> ZIO<R, Throwable, Nothing> exec(CheckedRunnable task) {
    return from(task.asProducer());
  }

  static <R, E, A> ZIO<R, E, A> pure(A value) {
    return env -> IO.pure(Either.right(value));
  }

  static <R, E, A> ZIO<R, E, A> raiseError(E error) {
    return env -> IO.pure(Either.left(error));
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
}
