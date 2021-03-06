/*
 * Copyright (c) 2018-2021, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import static com.github.tonivade.purefun.concurrent.FutureOf.toFuture;
import static com.github.tonivade.purefun.effect.RIOOf.toRIO;
import static com.github.tonivade.purefun.instances.FutureInstances.async;

import java.time.Duration;
import java.util.concurrent.Executor;

import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.concurrent.Future;
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
import com.github.tonivade.purefun.typeclasses.Runtime;

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

  static <R> MonadDefer<Kind<RIO_, R>> monadDefer() {
    return RIOMonadDefer.INSTANCE;
  }

  static <R> Console<Kind<Kind<RIO_, R>, Throwable>> console() {
    return RIOConsole.INSTANCE;
  }
  
  static <R> Runtime<Kind<RIO_, R>> runtime(R env) {
    return RIORuntime.instance(env);
  }
}

interface RIOFunctor<R> extends Functor<Kind<RIO_, R>> {

  @SuppressWarnings("rawtypes")
  RIOFunctor INSTANCE = new RIOFunctor() {};

  @Override
  default <A, B> RIO<R, B>
          map(Kind<Kind<RIO_, R>, ? extends A> value, Function1<? super A, ? extends B> map) {
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
          ap(Kind<Kind<RIO_, R>, ? extends A> value,
             Kind<Kind<RIO_, R>, ? extends Function1<? super A, ? extends B>> apply) {
    return value.fix(RIOOf::<R, A>narrowK).ap(apply.fix(RIOOf::narrowK));
  }
}

interface RIOMonad<R> extends RIOPure<R>, Monad<Kind<RIO_, R>> {

  @SuppressWarnings("rawtypes")
  RIOMonad INSTANCE = new RIOMonad() {};

  @Override
  default <A, B> RIO<R, B>
          flatMap(Kind<Kind<RIO_, R>, ? extends A> value,
                  Function1<? super A, ? extends Kind<Kind<RIO_, R>, ? extends B>> map) {
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
  default <A> RIO<R, A> handleErrorWith(
      Kind<Kind<RIO_, R>, A> value,
      Function1<? super Throwable, ? extends Kind<Kind<RIO_, R>, ? extends A>> handler) {
    // XXX: java8 fails to infer types, I have to do this in steps
    Function1<? super Throwable, RIO<R, A>> mapError = handler.andThen(RIOOf::narrowK);
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
          defer(Producer<? extends Kind<Kind<RIO_, R>, ? extends A>> defer) {
    return RIO.defer(() -> defer.map(RIOOf::<R, A>narrowK).get());
  }
}

interface RIOBracket<R> extends RIOMonadError<R>, Bracket<Kind<RIO_, R>, Throwable> {

  @Override
  default <A, B> RIO<R, B>
          bracket(Kind<Kind<RIO_, R>, ? extends A> acquire,
                  Function1<? super A, ? extends Kind<Kind<RIO_, R>, ? extends B>> use,
                  Consumer1<? super A> release) {
    return RIO.bracket(acquire.fix(toRIO()), use.andThen(RIOOf::narrowK), release);
  }
}

interface RIOMonadDefer<R>
    extends MonadDefer<Kind<RIO_, R>>, RIODefer<R>, RIOBracket<R> {

  @SuppressWarnings("rawtypes")
  RIOMonadDefer INSTANCE = new RIOMonadDefer<Object>() {};

  @Override
  default RIO<R, Unit> sleep(Duration duration) {
    return UIO.sleep(duration).<R>toRIO();
  }
}

final class RIOConsole<R> implements Console<Kind<RIO_, R>> {

  @SuppressWarnings("rawtypes")
  protected static final RIOConsole INSTANCE = new RIOConsole();

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

interface RIORuntime<R> extends Runtime<Kind<RIO_, R>> {
  
  static <R> RIORuntime<R> instance(R env) {
    return () -> env;
  }

  R env();

  @Override
  default <T> T run(Kind<Kind<RIO_, R>, T> value) {
    return value.fix(toRIO()).safeRunSync(env()).getOrElseThrow();
  }

  @Override
  default <T> Future<T> parRun(Kind<Kind<RIO_, R>, T> value, Executor executor) {
    return value.fix(toRIO()).foldMap(env(), async(executor)).fix(toFuture());
  }
}