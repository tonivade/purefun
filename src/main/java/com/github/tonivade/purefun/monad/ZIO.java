/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.monad;

import static com.github.tonivade.purefun.Function1.identity;

import com.github.tonivade.purefun.FlatMap3;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Higher2;
import com.github.tonivade.purefun.Higher3;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Nothing;
import com.github.tonivade.purefun.type.Either;

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
    return env -> provide(env).flatMap(either -> either.map(map.andThen(ZIO::narrowK)).fold(ZIO::failure, identity()).provide(env));
  }

  static <R, E, A> ZIO<R, E, A> accessM(Function1<R, ZIO<R, E, A>> map) {
    return env -> map.apply(env).provide(env);
  }

  static <R, A> ZIO<R, Nothing, A> access(Function1<R, A> map) {
    return accessM(map.andThen(ZIO::success));
  }

  static <R> ZIO<R, Nothing, R> get() {
    return access(identity());
  }

  static <R, E, A> ZIO<R, E, A> success(A value) {
    return environment -> IO.pure(Either.right(value));
  }

  static <R, E, A> ZIO<R, E, A> failure(E error) {
    return environment -> IO.pure(Either.left(error));
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
