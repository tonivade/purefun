/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import static com.github.tonivade.purefun.effect.URIOOf.toURIO;
import java.time.Duration;
import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.effect.UIO;
import com.github.tonivade.purefun.effect.URIO_;
import com.github.tonivade.purefun.effect.URIO;
import com.github.tonivade.purefun.effect.URIOOf;
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
import com.github.tonivade.purefun.typeclasses.Timer;

@SuppressWarnings("unchecked")
public interface URIOInstances {

  static <R> Functor<Kind<URIO_, R>> functor() {
    return URIOFunctor.INSTANCE;
  }

  static <R> Applicative<Kind<URIO_, R>> applicative() {
    return URIOApplicative.INSTANCE;
  }

  static <R> Monad<Kind<URIO_, R>> monad() {
    return URIOMonad.INSTANCE;
  }

  static <R> MonadThrow<Kind<URIO_, R>> monadThrow() {
    return URIOMonadThrow.INSTANCE;
  }
  
  static <R> Timer<Kind<URIO_, R>> timer() {
   return URIOTimer.INSTANCE;
  }

  static <R> MonadDefer<Kind<URIO_, R>> monadDefer() {
    return URIOMonadDefer.INSTANCE;
  }

  static <R, A> Reference<Kind<URIO_, R>, A> ref(A value) {
    return Reference.of(monadDefer(), value);
  }
  
  static <R, A extends AutoCloseable> Resource<Kind<URIO_, R>, A> resource(URIO<R, A> acquire) {
    return resource(acquire, AutoCloseable::close);
  }
  
  static <R, A> Resource<Kind<URIO_, R>, A> resource(URIO<R, A> acquire, Consumer1<A> release) {
    return Resource.from(monadDefer(), acquire, release);
  }

  static <R> Console<Kind<Kind<URIO_, R>, Throwable>> console() {
    return ConsoleURIO.INSTANCE;
  }
}

interface URIOFunctor<R> extends Functor<Kind<URIO_, R>> {

  @SuppressWarnings("rawtypes")
  URIOFunctor INSTANCE = new URIOFunctor() {};

  @Override
  default <A, B> URIO<R, B>
          map(Kind<Kind<URIO_, R>, A> value, Function1<A, B> map) {
    return URIOOf.narrowK(value).map(map);
  }
}

interface URIOPure<R> extends Applicative<Kind<URIO_, R>> {

  @Override
  default <A> URIO<R, A> pure(A value) {
    return URIO.<R, A>pure(value);
  }
}

interface URIOApplicative<R> extends URIOPure<R> {

  @SuppressWarnings("rawtypes")
  URIOApplicative INSTANCE = new URIOApplicative() {};

  @Override
  default <A, B> URIO<R, B>
          ap(Kind<Kind<URIO_, R>, A> value,
             Kind<Kind<URIO_, R>, Function1<A, B>> apply) {
    return URIOOf.narrowK(apply).flatMap(map -> URIOOf.narrowK(value).map(map));
  }
}

interface URIOMonad<R> extends URIOPure<R>, Monad<Kind<URIO_, R>> {

  @SuppressWarnings("rawtypes")
  URIOMonad INSTANCE = new URIOMonad() {};

  @Override
  default <A, B> URIO<R, B>
          flatMap(Kind<Kind<URIO_, R>, A> value,
                  Function1<A, ? extends Kind<Kind<URIO_, R>, B>> map) {
    return value.fix(toURIO()).flatMap(map.andThen(URIOOf::narrowK));
  }
}

interface URIOMonadError<R> extends URIOMonad<R>, MonadError<Kind<URIO_, R>, Throwable> {

  @SuppressWarnings("rawtypes")
  URIOMonadError INSTANCE = new URIOMonadError<Object>() {};

  @Override
  default <A> URIO<R, A> raiseError(Throwable error) {
    return URIO.<R, A>raiseError(error);
  }

  @Override
  default <A> URIO<R, A>
          handleErrorWith(Kind<Kind<URIO_, R>, A> value,
                          Function1<Throwable, ? extends Kind<Kind<URIO_, R>, A>> handler) {
    // XXX: java8 fails to infer types, I have to do this in steps
    Function1<Throwable, URIO<R, A>> mapError = handler.andThen(URIOOf::narrowK);
    Function1<A, URIO<R, A>> map = URIO::pure;
    URIO<R, A> urio = URIOOf.narrowK(value);
    return urio.redeemWith(mapError, map);
  }
}

interface URIOMonadThrow<R>
    extends URIOMonadError<R>,
            MonadThrow<Kind<URIO_, R>> {
  @SuppressWarnings("rawtypes")
  URIOMonadThrow INSTANCE = new URIOMonadThrow<Object>() {};
}

interface URIODefer<R> extends Defer<Kind<URIO_, R>> {

  @Override
  default <A> URIO<R, A>
          defer(Producer<Kind<Kind<URIO_, R>, A>> defer) {
    return URIO.defer(() -> defer.map(URIOOf::narrowK).get());
  }
}

interface URIOBracket<R> extends Bracket<Kind<URIO_, R>> {

  @Override
  default <A, B> URIO<R, B>
          bracket(Kind<Kind<URIO_, R>, A> acquire,
                  Function1<A, ? extends Kind<Kind<URIO_, R>, B>> use,
                  Consumer1<A> release) {
    return URIO.bracket(acquire.fix(toURIO()), use.andThen(URIOOf::narrowK), release);
  }
}

interface URIOTimer<R> extends Timer<Kind<URIO_, R>> {
  
  @SuppressWarnings("rawtypes")
  URIOTimer INSTANCE = new URIOTimer() {};

  @Override
  default URIO<R, Unit> sleep(Duration duration) {
    return UIO.sleep(duration).<R>toURIO();
  }
}

interface URIOMonadDefer<R>
    extends MonadDefer<Kind<URIO_, R>>, URIOMonadThrow<R>, URIODefer<R>, URIOBracket<R>, URIOTimer<R> {

  @SuppressWarnings("rawtypes")
  URIOMonadDefer INSTANCE = new URIOMonadDefer<Object>() {};
}

final class ConsoleURIO<R> implements Console<Kind<URIO_, R>> {

  @SuppressWarnings("rawtypes")
  protected static final ConsoleURIO INSTANCE = new ConsoleURIO();

  private final SystemConsole console = new SystemConsole();

  @Override
  public URIO<R, String> readln() {
    return URIO.<R, String>task(console::readln);
  }

  @Override
  public URIO<R, Unit> println(String text) {
    return URIO.<R>exec(() -> console.println(text));
  }
}
