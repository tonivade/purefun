/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import java.time.Duration;
import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Higher3;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.effect.UIO;
import com.github.tonivade.purefun.effect.ZIO;
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

@SuppressWarnings("unchecked")
public interface ZIOInstances {

  static <R, E> Functor<Higher1<Higher1<ZIO_, R>, E>> functor() {
    return ZIOFunctor.INSTANCE;
  }

  static <R, E> Applicative<Higher1<Higher1<ZIO_, R>, E>> applicative() {
    return ZIOApplicative.INSTANCE;
  }

  static <R, E> Monad<Higher1<Higher1<ZIO_, R>, E>> monad() {
    return ZIOMonad.INSTANCE;
  }

  static <R, E> MonadError<Higher1<Higher1<ZIO_, R>, E>, E> monadError() {
    return ZIOMonadError.INSTANCE;
  }

  static <R> MonadThrow<Higher1<Higher1<ZIO_, R>, Throwable>> monadThrow() {
    return ZIOMonadThrow.INSTANCE;
  }

  static <R> MonadDefer<Higher1<Higher1<ZIO_, R>, Throwable>> monadDefer() {
    return ZIOMonadDefer.INSTANCE;
  }

  static <R, A> Reference<Higher1<Higher1<ZIO_, R>, Throwable>, A> ref(A value) {
    return Reference.of(monadDefer(), value);
  }

  static <R> Console<Higher1<Higher1<ZIO_, R>, Throwable>> console() {
    return ConsoleZIO.INSTANCE;
  }
}

interface ZIOFunctor<R, E> extends Functor<Higher1<Higher1<ZIO_, R>, E>> {

  @SuppressWarnings("rawtypes")
  ZIOFunctor INSTANCE = new ZIOFunctor() {};

  @Override
  default <A, B> Higher3<ZIO_, R, E, B>
          map(Higher1<Higher1<Higher1<ZIO_, R>, E>, A> value, Function1<A, B> map) {
    return ZIO_.narrowK(value).map(map);
  }
}

interface ZIOPure<R, E> extends Applicative<Higher1<Higher1<ZIO_, R>, E>> {

  @Override
  default <A> Higher3<ZIO_, R, E, A> pure(A value) {
    return ZIO.<R, E, A>pure(value);
  }
}

interface ZIOApplicative<R, E> extends ZIOPure<R, E> {

  @SuppressWarnings("rawtypes")
  ZIOApplicative INSTANCE = new ZIOApplicative() {};

  @Override
  default <A, B> Higher3<ZIO_, R, E, B>
          ap(Higher1<Higher1<Higher1<ZIO_, R>, E>, A> value,
             Higher1<Higher1<Higher1<ZIO_, R>, E>, Function1<A, B>> apply) {
    return ZIO_.narrowK(apply).flatMap(map -> ZIO_.narrowK(value).map(map));
  }
}

interface ZIOMonad<R, E> extends ZIOPure<R, E>, Monad<Higher1<Higher1<ZIO_, R>, E>> {

  @SuppressWarnings("rawtypes")
  ZIOMonad INSTANCE = new ZIOMonad() {};

  @Override
  default <A, B> Higher3<ZIO_, R, E, B>
          flatMap(Higher1<Higher1<Higher1<ZIO_, R>, E>, A> value,
                  Function1<A, ? extends Higher1<Higher1<Higher1<ZIO_, R>, E>, B>> map) {
    return ZIO_.narrowK(value).flatMap(map.andThen(ZIO_::narrowK));
  }
}

interface ZIOMonadError<R, E> extends ZIOMonad<R, E>, MonadError<Higher1<Higher1<ZIO_, R>, E>, E> {

  @SuppressWarnings("rawtypes")
  ZIOMonadError INSTANCE = new ZIOMonadError() {};

  @Override
  default <A> Higher3<ZIO_, R, E, A> raiseError(E error) {
    return ZIO.<R, E, A>raiseError(error);
  }

  @Override
  default <A> Higher3<ZIO_, R, E, A>
          handleErrorWith(Higher1<Higher1<Higher1<ZIO_, R>, E>, A> value,
                          Function1<E, ? extends Higher1<Higher1<Higher1<ZIO_, R>, E>, A>> handler) {
    // XXX: java8 fails to infer types, I have to do this in steps
    Function1<E, ZIO<R, E, A>> mapError = handler.andThen(ZIO_::narrowK);
    Function1<A, ZIO<R, E, A>> map = ZIO::pure;
    ZIO<R, E, A> zio = ZIO_.narrowK(value);
    return zio.foldM(mapError, map);
  }
}

interface ZIOMonadThrow<R>
    extends ZIOMonadError<R, Throwable>,
            MonadThrow<Higher1<Higher1<ZIO_, R>, Throwable>> {
  @SuppressWarnings("rawtypes")
  ZIOMonadThrow INSTANCE = new ZIOMonadThrow() {};
}

interface ZIODefer<R> extends Defer<Higher1<Higher1<ZIO_, R>, Throwable>> {

  @Override
  default <A> Higher3<ZIO_, R, Throwable, A>
          defer(Producer<Higher1<Higher1<Higher1<ZIO_, R>, Throwable>, A>> defer) {
    return ZIO.defer(() -> defer.map(ZIO_::narrowK).get());
  }
}

interface ZIOBracket<R> extends Bracket<Higher1<Higher1<ZIO_, R>, Throwable>> {

  @Override
  default <A, B> Higher3<ZIO_, R, Throwable, B>
          bracket(Higher1<Higher1<Higher1<ZIO_, R>, Throwable>, A> acquire,
                  Function1<A, ? extends Higher1<Higher1<Higher1<ZIO_, R>, Throwable>, B>> use,
                  Consumer1<A> release) {
    return ZIO.bracket(acquire.fix1(ZIO_::narrowK), use.andThen(ZIO_::narrowK), release);
  }
}

interface ZIOMonadDefer<R>
    extends MonadDefer<Higher1<Higher1<ZIO_, R>, Throwable>>, ZIOMonadThrow<R>, ZIODefer<R>, ZIOBracket<R> {

  @SuppressWarnings("rawtypes")
  ZIOMonadDefer INSTANCE = new ZIOMonadDefer() {};

  @Override
  default Higher3<ZIO_, R, Throwable, Unit> sleep(Duration duration) {
    return UIO.sleep(duration).<R, Throwable>toZIO();
  }
}

final class ConsoleZIO<R> implements Console<Higher1<Higher1<ZIO_, R>, Throwable>> {

  @SuppressWarnings("rawtypes")
  protected static final ConsoleZIO INSTANCE = new ConsoleZIO();

  private final SystemConsole console = new SystemConsole();

  @Override
  public Higher1<Higher1<Higher1<ZIO_, R>, Throwable>, String> readln() {
    return ZIO.<R, String>task(console::readln);
  }

  @Override
  public Higher1<Higher1<Higher1<ZIO_, R>, Throwable>, Unit> println(String text) {
    return ZIO.<R>exec(() -> console.println(text));
  }
}
