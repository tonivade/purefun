/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import java.time.Duration;
import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.effect.UIO;
import com.github.tonivade.purefun.effect.ZIO;
import com.github.tonivade.purefun.effect.ZIOOf;
import com.github.tonivade.purefun.effect.ZIO_;
import com.github.tonivade.purefun.typeclasses.Applicative;
import com.github.tonivade.purefun.typeclasses.Bracket;
import com.github.tonivade.purefun.typeclasses.Console;
import com.github.tonivade.purefun.typeclasses.Defer;
import com.github.tonivade.purefun.typeclasses.Functor;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.MonadDefer;
import com.github.tonivade.purefun.typeclasses.MonadError;
import com.github.tonivade.purefun.typeclasses.MonadThrow;
import com.github.tonivade.purefun.typeclasses.Reference;
import com.github.tonivade.purefun.typeclasses.Resource;

@SuppressWarnings("unchecked")
public interface ZIOInstances {

  static <R, E> Functor<Kind<Kind<ZIO_, R>, E>> functor() {
    return ZIOFunctor.INSTANCE;
  }

  static <R, E> Applicative<Kind<Kind<ZIO_, R>, E>> applicative() {
    return ZIOApplicative.INSTANCE;
  }

  static <R, E> Monad<Kind<Kind<ZIO_, R>, E>> monad() {
    return ZIOMonad.INSTANCE;
  }

  static <R, E> MonadError<Kind<Kind<ZIO_, R>, E>, E> monadError() {
    return ZIOMonadError.INSTANCE;
  }

  static <R> MonadThrow<Kind<Kind<ZIO_, R>, Throwable>> monadThrow() {
    return ZIOMonadThrow.INSTANCE;
  }

  static <R> MonadDefer<Kind<Kind<ZIO_, R>, Throwable>> monadDefer() {
    return ZIOMonadDefer.INSTANCE;
  }

  static <R, A> Reference<Kind<Kind<ZIO_, R>, Throwable>, A> ref(A value) {
    return Reference.of(monadDefer(), value);
  }
  
  static <R, A extends AutoCloseable> Resource<Kind<Kind<ZIO_, R>, Throwable>, A> resource(
      ZIO<R, Throwable, A> acquire) {
    return resource(acquire, AutoCloseable::close);
  }
  
  static <R, A> Resource<Kind<Kind<ZIO_, R>, Throwable>, A> resource(
      ZIO<R, Throwable, A> acquire, Consumer1<A> release) {
    return Resource.from(monadDefer(), acquire, release);
  }

  static <R> Console<Kind<Kind<ZIO_, R>, Throwable>> console() {
    return ConsoleZIO.INSTANCE;
  }
}

interface ZIOFunctor<R, E> extends Functor<Kind<Kind<ZIO_, R>, E>> {

  @SuppressWarnings("rawtypes")
  ZIOFunctor INSTANCE = new ZIOFunctor() {};

  @Override
  default <A, B> ZIO<R, E, B>
          map(Kind<Kind<Kind<ZIO_, R>, E>, A> value, Function1<A, B> map) {
    return ZIOOf.narrowK(value).map(map);
  }
}

interface ZIOPure<R, E> extends Applicative<Kind<Kind<ZIO_, R>, E>> {

  @Override
  default <A> ZIO<R, E, A> pure(A value) {
    return ZIO.<R, E, A>pure(value);
  }
}

interface ZIOApplicative<R, E> extends ZIOPure<R, E> {

  @SuppressWarnings("rawtypes")
  ZIOApplicative INSTANCE = new ZIOApplicative() {};

  @Override
  default <A, B> ZIO<R, E, B>
          ap(Kind<Kind<Kind<ZIO_, R>, E>, A> value,
             Kind<Kind<Kind<ZIO_, R>, E>, Function1<A, B>> apply) {
    return ZIOOf.narrowK(apply).flatMap(map -> ZIOOf.narrowK(value).map(map));
  }
}

interface ZIOMonad<R, E> extends ZIOPure<R, E>, Monad<Kind<Kind<ZIO_, R>, E>> {

  @SuppressWarnings("rawtypes")
  ZIOMonad INSTANCE = new ZIOMonad() {};

  @Override
  default <A, B> ZIO<R, E, B>
          flatMap(Kind<Kind<Kind<ZIO_, R>, E>, A> value,
                  Function1<A, ? extends Kind<Kind<Kind<ZIO_, R>, E>, B>> map) {
    return ZIOOf.narrowK(value).flatMap(map.andThen(ZIOOf::narrowK));
  }
}

interface ZIOMonadError<R, E> extends ZIOMonad<R, E>, MonadError<Kind<Kind<ZIO_, R>, E>, E> {

  @SuppressWarnings("rawtypes")
  ZIOMonadError INSTANCE = new ZIOMonadError() {};

  @Override
  default <A> ZIO<R, E, A> raiseError(E error) {
    return ZIO.<R, E, A>raiseError(error);
  }

  @Override
  default <A> ZIO<R, E, A>
          handleErrorWith(Kind<Kind<Kind<ZIO_, R>, E>, A> value,
                          Function1<E, ? extends Kind<Kind<Kind<ZIO_, R>, E>, A>> handler) {
    // XXX: java8 fails to infer types, I have to do this in steps
    Function1<E, ZIO<R, E, A>> mapError = handler.andThen(ZIOOf::narrowK);
    Function1<A, ZIO<R, E, A>> map = ZIO::pure;
    ZIO<R, E, A> zio = ZIOOf.narrowK(value);
    return zio.foldM(mapError, map);
  }
}

interface ZIOMonadThrow<R>
    extends ZIOMonadError<R, Throwable>,
            MonadThrow<Kind<Kind<ZIO_, R>, Throwable>> {
  @SuppressWarnings("rawtypes")
  ZIOMonadThrow INSTANCE = new ZIOMonadThrow() {};
}

interface ZIODefer<R> extends Defer<Kind<Kind<ZIO_, R>, Throwable>> {

  @Override
  default <A> ZIO<R, Throwable, A>
          defer(Producer<Kind<Kind<Kind<ZIO_, R>, Throwable>, A>> defer) {
    return ZIO.defer(() -> defer.map(ZIOOf::narrowK).get());
  }
}

interface ZIOBracket<R> extends Bracket<Kind<Kind<ZIO_, R>, Throwable>> {

  @Override
  default <A, B> ZIO<R, Throwable, B>
          bracket(Kind<Kind<Kind<ZIO_, R>, Throwable>, A> acquire,
                  Function1<A, ? extends Kind<Kind<Kind<ZIO_, R>, Throwable>, B>> use,
                  Consumer1<A> release) {
    return ZIO.bracket(acquire.fix(ZIOOf::narrowK), use.andThen(ZIOOf::narrowK), release);
  }
}

interface ZIOMonadDefer<R>
    extends MonadDefer<Kind<Kind<ZIO_, R>, Throwable>>, ZIOMonadThrow<R>, ZIODefer<R>, ZIOBracket<R> {

  @SuppressWarnings("rawtypes")
  ZIOMonadDefer INSTANCE = new ZIOMonadDefer() {};

  @Override
  default ZIO<R, Throwable, Unit> sleep(Duration duration) {
    return UIO.sleep(duration).<R, Throwable>toZIO();
  }
}

final class ConsoleZIO<R> implements Console<Kind<Kind<ZIO_, R>, Throwable>> {

  @SuppressWarnings("rawtypes")
  protected static final ConsoleZIO INSTANCE = new ConsoleZIO();

  private final SystemConsole console = new SystemConsole();

  @Override
  public ZIO<R, Throwable, String> readln() {
    return ZIO.<R, String>task(console::readln);
  }

  @Override
  public ZIO<R, Throwable, Unit> println(String text) {
    return ZIO.<R>exec(() -> console.println(text));
  }
}
