/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Higher3;
import com.github.tonivade.purefun.Instance;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Unit;
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
import com.github.tonivade.purefun.effect.ZIO;

@SuppressWarnings("unchecked")
public interface ZIOInstances {

  static <R, E> Functor<Higher1<Higher1<ZIO.µ, R>, E>> functor() {
    return (ZIOFunctor<R, E>) ZIOFunctor.INSTANCE;
  }

  static <R, E> Applicative<Higher1<Higher1<ZIO.µ, R>, E>> applicative() {
    return (ZIOApplicative<R, E>) ZIOApplicative.INSTANCE;
  }

  static <R, E> Monad<Higher1<Higher1<ZIO.µ, R>, E>> monad() {
    return (ZIOMonad<R, E>) ZIOMonad.INSTANCE;
  }

  static <R, E> MonadError<Higher1<Higher1<ZIO.µ, R>, E>, E> monadError() {
    return (ZIOMonadError<R, E>) ZIOMonadError.INSTANCE;
  }

  static <R> MonadThrow<Higher1<Higher1<ZIO.µ, R>, Throwable>> monadThrow() {
    return (ZIOMonadThrow<R>) ZIOMonadThrow.INSTANCE;
  }

  static <R> MonadDefer<Higher1<Higher1<ZIO.µ, R>, Throwable>> monadDefer() {
    return (ZIOMonadDefer<R>) ZIOMonadDefer.INSTANCE;
  }

  static <R, A> Reference<Higher1<Higher1<ZIO.µ, R>, Throwable>, A> ref(A value) {
    return Reference.of(monadDefer(), value);
  }
}

@Instance
interface ZIOFunctor<R, E> extends Functor<Higher1<Higher1<ZIO.µ, R>, E>> {

  ZIOFunctor<?, ?> INSTANCE = new ZIOFunctor<Object, Object>() { };

  @Override
  default <A, B> Higher3<ZIO.µ, R, E, B>
          map(Higher1<Higher1<Higher1<ZIO.µ, R>, E>, A> value, Function1<A, B> map) {
    return ZIO.narrowK(value).map(map).kind3();
  }
}

interface ZIOPure<R, E> extends Applicative<Higher1<Higher1<ZIO.µ, R>, E>> {

  @Override
  default <A> Higher3<ZIO.µ, R, E, A> pure(A value) {
    return ZIO.<R, E, A>pure(value).kind3();
  }
}

@Instance
interface ZIOApplicative<R, E> extends ZIOPure<R, E> {

  ZIOApplicative<?, ?> INSTANCE = new ZIOApplicative<Object, Object>() { };

  @Override
  default <A, B> Higher3<ZIO.µ, R, E, B>
          ap(Higher1<Higher1<Higher1<ZIO.µ, R>, E>, A> value,
             Higher1<Higher1<Higher1<ZIO.µ, R>, E>, Function1<A, B>> apply) {
    return ZIO.narrowK(apply).flatMap(map -> ZIO.narrowK(value).map(map)).kind3();
  }
}

@Instance
interface ZIOMonad<R, E> extends ZIOPure<R, E>, Monad<Higher1<Higher1<ZIO.µ, R>, E>> {

  ZIOMonad<?, ?> INSTANCE = new ZIOMonad<Object, Object>() { };

  @Override
  default <A, B> Higher3<ZIO.µ, R, E, B>
          flatMap(Higher1<Higher1<Higher1<ZIO.µ, R>, E>, A> value,
                  Function1<A, ? extends Higher1<Higher1<Higher1<ZIO.µ, R>, E>, B>> map) {
    return ZIO.narrowK(value).flatMap(map.andThen(ZIO::narrowK)).kind3();
  }
}

@Instance
interface ZIOMonadError<R, E> extends ZIOMonad<R, E>, MonadError<Higher1<Higher1<ZIO.µ, R>, E>, E> {

  ZIOMonadError<?, ?> INSTANCE = new ZIOMonadError<Object, Object>() { };

  @Override
  default <A> Higher3<ZIO.µ, R, E, A> raiseError(E error) {
    return ZIO.<R, E, A>raiseError(error).kind3();
  }

  @Override
  default <A> Higher3<ZIO.µ, R, E, A>
          handleErrorWith(Higher1<Higher1<Higher1<ZIO.µ, R>, E>, A> value,
                          Function1<E, ? extends Higher1<Higher1<Higher1<ZIO.µ, R>, E>, A>> handler) {
    // XXX: java8 fails to infer types, I have to do this in steps
    Function1<E, ZIO<R, E, A>> mapError = handler.andThen(ZIO::narrowK);
    Function1<A, ZIO<R, E, A>> map = ZIO::pure;
    ZIO<R, E, A> zio = ZIO.narrowK(value);
    return zio.foldM(mapError, map).kind3();
  }
}

@Instance
interface ZIOMonadThrow<R>
    extends ZIOMonadError<R, Throwable>,
            MonadThrow<Higher1<Higher1<ZIO.µ, R>, Throwable>> {

  ZIOMonadThrow<?> INSTANCE = new ZIOMonadThrow<Object>() { };
}

@Instance
interface ZIODefer<R> extends Defer<Higher1<Higher1<ZIO.µ, R>, Throwable>> {

  ZIODefer<?> INSTANCE = new ZIODefer<Object>() { };

  @Override
  default <A> Higher3<ZIO.µ, R, Throwable, A>
          defer(Producer<Higher1<Higher1<Higher1<ZIO.µ, R>, Throwable>, A>> defer) {
    return ZIO.defer(() -> defer.map(ZIO::narrowK).get()).kind3();
  }
}

@Instance
interface ZIOBracket<R> extends Bracket<Higher1<Higher1<ZIO.µ, R>, Throwable>> {

  ZIOBracket<?> INSTANCE = new ZIOBracket<Object>() { };

  @Override
  default <A, B> Higher3<ZIO.µ, R, Throwable, B>
          bracket(Higher1<Higher1<Higher1<ZIO.µ, R>, Throwable>, A> acquire,
                  Function1<A, ? extends Higher1<Higher1<Higher1<ZIO.µ, R>, Throwable>, B>> use,
                  Consumer1<A> release) {
    return ZIO.bracket(acquire.fix1(ZIO::narrowK), use.andThen(ZIO::narrowK), release).kind3();
  }
}

@Instance
interface ZIOMonadDefer<R>
    extends MonadDefer<Higher1<Higher1<ZIO.µ, R>, Throwable>>,
            ZIOMonadThrow<R>,
            ZIODefer<R>,
            ZIOBracket<R> {

  ZIOMonadDefer<?> INSTANCE = new ZIOMonadDefer<Object>() { };
}

@Instance
final class ConsoleZIO<R> implements Console<Higher1<Higher1<ZIO.µ, R>, Throwable>> {

  private final SystemConsole console = new SystemConsole();

  @Override
  public Higher1<Higher1<Higher1<ZIO.µ, R>, Throwable>, String> readln() {
    return ZIO.<R, Throwable, String>task(console::readln).kind1();
  }

  @Override
  public Higher1<Higher1<Higher1<ZIO.µ, R>, Throwable>, Unit> println(String text) {
    return ZIO.<R>exec(() -> console.println(text)).kind1();
  }
}
