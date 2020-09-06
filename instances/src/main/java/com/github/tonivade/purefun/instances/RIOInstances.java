/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import static com.github.tonivade.purefun.effect.RIOOf.toRIO;
import java.time.Duration;
import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.effect.RIO;
import com.github.tonivade.purefun.effect.RIOOf;
import com.github.tonivade.purefun.effect.RIO_;
import com.github.tonivade.purefun.effect.UIO;
import com.github.tonivade.purefun.typeclasses.Applicative;
import com.github.tonivade.purefun.typeclasses.Bracket;
import com.github.tonivade.purefun.typeclasses.Console;
import com.github.tonivade.purefun.typeclasses.Defer;
import com.github.tonivade.purefun.typeclasses.Functor;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.MonadDefer;
import com.github.tonivade.purefun.typeclasses.MonadError;
import com.github.tonivade.purefun.typeclasses.MonadThrow;
import com.github.tonivade.purefun.typeclasses.Timer;

@SuppressWarnings("unchecked")
public interface RIOInstances {

  static <R> Functor<Kind<RIO_, R>> functor() {
    return RIOFunctor.INSTANCE;
  }

  static <R> Applicative<Kind<RIO_, R>> applicative() {
    return RIOApplicative.INSTANCE;
  }

  static <R> Monad<Kind<RIO_, R>> monad() {
    return RIOMonad.INSTANCE;
  }

  static <R> MonadThrow<Kind<RIO_, R>> monadThrow() {
    return RIOMonadThrow.INSTANCE;
  }
  
  static <R> Timer<Kind<RIO_, R>> timer() {
   return RIOTimer.INSTANCE;
  }

  static <R> MonadDefer<Kind<RIO_, R>> monadDefer() {
    return RIOMonadDefer.INSTANCE;
  }

  static <R> Console<Kind<Kind<RIO_, R>, Throwable>> console() {
    return ConsoleRIO.INSTANCE;
  }
}

interface RIOFunctor<R> extends Functor<Kind<RIO_, R>> {

  @SuppressWarnings("rawtypes")
  RIOFunctor INSTANCE = new RIOFunctor() {};

  @Override
  default <A, B> RIO<R, B>
          map(Kind<Kind<RIO_, R>, A> value, Function1<A, B> map) {
    return RIOOf.narrowK(value).map(map);
  }
}

interface RIOPure<R> extends Applicative<Kind<RIO_, R>> {

  @Override
  default <A> RIO<R, A> pure(A value) {
    return RIO.<R, A>pure(value);
  }
}

interface RIOApplicative<R> extends RIOPure<R> {

  @SuppressWarnings("rawtypes")
  RIOApplicative INSTANCE = new RIOApplicative() {};

  @Override
  default <A, B> RIO<R, B>
          ap(Kind<Kind<RIO_, R>, A> value,
             Kind<Kind<RIO_, R>, Function1<A, B>> apply) {
    return RIOOf.narrowK(apply).flatMap(map -> RIOOf.narrowK(value).map(map));
  }
}

interface RIOMonad<R> extends RIOPure<R>, Monad<Kind<RIO_, R>> {

  @SuppressWarnings("rawtypes")
  RIOMonad INSTANCE = new RIOMonad() {};

  @Override
  default <A, B> RIO<R, B>
          flatMap(Kind<Kind<RIO_, R>, A> value,
                  Function1<A, ? extends Kind<Kind<RIO_, R>, B>> map) {
    return value.fix(toRIO()).flatMap(map.andThen(RIOOf::narrowK));
  }
}

interface RIOMonadError<R> extends RIOMonad<R>, MonadError<Kind<RIO_, R>, Throwable> {

  @SuppressWarnings("rawtypes")
  RIOMonadError INSTANCE = new RIOMonadError<Object>() {};

  @Override
  default <A> RIO<R, A> raiseError(Throwable error) {
    return RIO.<R, A>raiseError(error);
  }

  @Override
  default <A> RIO<R, A>
          handleErrorWith(Kind<Kind<RIO_, R>, A> value,
                          Function1<Throwable, ? extends Kind<Kind<RIO_, R>, A>> handler) {
    // XXX: java8 fails to infer types, I have to do this in steps
    Function1<Throwable, RIO<R, A>> mapError = handler.andThen(RIOOf::narrowK);
    Function1<A, RIO<R, A>> map = RIO::pure;
    RIO<R, A> urio = RIOOf.narrowK(value);
    return urio.foldM(mapError, map);
  }
}

interface RIOMonadThrow<R>
    extends RIOMonadError<R>,
            MonadThrow<Kind<RIO_, R>> {
  @SuppressWarnings("rawtypes")
  RIOMonadThrow INSTANCE = new RIOMonadThrow<Object>() {};
}

interface RIODefer<R> extends Defer<Kind<RIO_, R>> {

  @Override
  default <A> RIO<R, A>
          defer(Producer<Kind<Kind<RIO_, R>, A>> defer) {
    return RIO.defer(() -> defer.map(RIOOf::narrowK).get());
  }
}

interface RIOBracket<R> extends Bracket<Kind<RIO_, R>> {

  @Override
  default <A, B> RIO<R, B>
          bracket(Kind<Kind<RIO_, R>, A> acquire,
                  Function1<A, ? extends Kind<Kind<RIO_, R>, B>> use,
                  Consumer1<A> release) {
    return RIO.bracket(acquire.fix(toRIO()), use.andThen(RIOOf::narrowK), release);
  }
}

interface RIOTimer<R> extends Timer<Kind<RIO_, R>> {
  
  @SuppressWarnings("rawtypes")
  RIOTimer INSTANCE = new RIOTimer() {};

  @Override
  default RIO<R, Unit> sleep(Duration duration) {
    return UIO.sleep(duration).<R>toRIO();
  }
}

interface RIOMonadDefer<R>
    extends MonadDefer<Kind<RIO_, R>>, RIOMonadThrow<R>, RIODefer<R>, RIOBracket<R>, RIOTimer<R> {

  @SuppressWarnings("rawtypes")
  RIOMonadDefer INSTANCE = new RIOMonadDefer<Object>() {};
}

final class ConsoleRIO<R> implements Console<Kind<RIO_, R>> {

  @SuppressWarnings("rawtypes")
  protected static final ConsoleRIO INSTANCE = new ConsoleRIO();

  private final SystemConsole console = new SystemConsole();

  @Override
  public RIO<R, String> readln() {
    return RIO.<R, String>task(console::readln);
  }

  @Override
  public RIO<R, Unit> println(String text) {
    return RIO.<R>exec(() -> console.println(text));
  }
}
