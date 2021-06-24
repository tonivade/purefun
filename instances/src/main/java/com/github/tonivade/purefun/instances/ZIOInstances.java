/*
 * Copyright (c) 2018-2021, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import static com.github.tonivade.purefun.effect.ZIOOf.toZIO;
import java.time.Duration;
import java.util.concurrent.Executor;
import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.concurrent.Future;
import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.effect.UIO;
import com.github.tonivade.purefun.effect.ZIO;
import com.github.tonivade.purefun.effect.ZIOOf;
import com.github.tonivade.purefun.effect.ZIO_;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Try;
import com.github.tonivade.purefun.typeclasses.Applicative;
import com.github.tonivade.purefun.typeclasses.Async;
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

  static <R> Async<Kind<Kind<ZIO_, R>, Throwable>> async() {
    return ZIOAsync.INSTANCE;
  }

  static <R> Console<Kind<Kind<ZIO_, R>, Throwable>> console() {
    return ZIOConsole.INSTANCE;
  }
  
  static <R, E> Runtime<Kind<Kind<ZIO_, R>, E>> runtime(R env) {
    return ZIORuntime.instance(env);
  }
}

interface ZIOFunctor<R, E> extends Functor<Kind<Kind<ZIO_, R>, E>> {

  @SuppressWarnings("rawtypes")
  ZIOFunctor INSTANCE = new ZIOFunctor() {};

  @Override
  default <A, B> ZIO<R, E, B> map(
      Kind<Kind<Kind<ZIO_, R>, E>, ? extends A> value, 
      Function1<? super A, ? extends B> map) {
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
          ap(Kind<Kind<Kind<ZIO_, R>, E>, ? extends A> value,
             Kind<Kind<Kind<ZIO_, R>, E>, ? extends Function1<? super A, ? extends B>> apply) {
    return value.fix(ZIOOf::<R, E, A>narrowK).ap(apply.fix(ZIOOf::narrowK));
  }
}

interface ZIOMonad<R, E> extends ZIOPure<R, E>, Monad<Kind<Kind<ZIO_, R>, E>> {

  @SuppressWarnings("rawtypes")
  ZIOMonad INSTANCE = new ZIOMonad() {};

  @Override
  default <A, B> ZIO<R, E, B>
          flatMap(Kind<Kind<Kind<ZIO_, R>, E>, ? extends A> value,
                  Function1<? super A, ? extends Kind<Kind<Kind<ZIO_, R>, E>, ? extends B>> map) {
    return value.fix(toZIO()).flatMap(map.andThen(ZIOOf::narrowK));
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
  default <A> ZIO<R, E, A> handleErrorWith(
      Kind<Kind<Kind<ZIO_, R>, E>, A> value,
      Function1<? super E, ? extends Kind<Kind<Kind<ZIO_, R>, E>, ? extends A>> handler) {
    // XXX: java8 fails to infer types, I have to do this in steps
    Function1<? super E, ZIO<R, E, A>> mapError = handler.andThen(ZIOOf::narrowK);
    Function1<A, ZIO<R, E, A>> map = ZIO::pure;
    ZIO<R, E, A> zio = ZIOOf.narrowK(value);
    return zio.foldM(mapError, map);
  }
}

interface ZIOMonadThrow<R>
    extends ZIOMonadError<R, Throwable>, MonadThrow<Kind<Kind<ZIO_, R>, Throwable>> {
  @SuppressWarnings("rawtypes")
  ZIOMonadThrow INSTANCE = new ZIOMonadThrow() {};
}

interface ZIODefer<R, E> extends Defer<Kind<Kind<ZIO_, R>, E>> {

  @Override
  default <A> ZIO<R, E, A>
          defer(Producer<? extends Kind<Kind<Kind<ZIO_, R>, E>, ? extends A>> defer) {
    return ZIO.defer(() -> defer.map(ZIOOf::<R, E, A>narrowK).get());
  }
}

interface ZIOBracket<R, E> extends ZIOMonadError<R, E>, Bracket<Kind<Kind<ZIO_, R>, E>, E> {

  @Override
  default <A, B> ZIO<R, E, B>
          bracket(Kind<Kind<Kind<ZIO_, R>, E>, ? extends A> acquire,
                  Function1<? super A, ? extends Kind<Kind<Kind<ZIO_, R>, E>, ? extends B>> use,
                  Function1<? super A, ? extends Kind<Kind<Kind<ZIO_, R>, E>, Unit>> release) {
    return ZIO.bracket(acquire.fix(toZIO()), use.andThen(ZIOOf::narrowK), release::apply);
  }
}

interface ZIOMonadDefer<R>
    extends MonadDefer<Kind<Kind<ZIO_, R>, Throwable>>, ZIODefer<R, Throwable>, ZIOBracket<R, Throwable> {

  @SuppressWarnings("rawtypes")
  ZIOMonadDefer INSTANCE = new ZIOMonadDefer() {};

  @Override
  default ZIO<R, Throwable, Unit> sleep(Duration duration) {
    return UIO.sleep(duration).<R, Throwable>toZIO();
  }
}

interface ZIOAsync<R> extends Async<Kind<Kind<ZIO_, R>, Throwable>>, ZIOMonadDefer<R> {

  @SuppressWarnings("rawtypes")
  ZIOAsync INSTANCE = new ZIOAsync<Object>() {};
  
  @Override
  default <A> ZIO<R, Throwable, A> asyncF(Function1<Consumer1<? super Try<? extends A>>, Kind<Kind<Kind<ZIO_, R>, Throwable>, Unit>> consumer) {
    return ZIO.cancellable((env, cb) -> consumer.andThen(ZIOOf::narrowK).apply(e -> cb.accept(Try.success(e.toEither()))));
  }
}

final class ZIOConsole<R> implements Console<Kind<Kind<ZIO_, R>, Throwable>> {

  @SuppressWarnings("rawtypes")
  protected static final ZIOConsole INSTANCE = new ZIOConsole();

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

interface ZIORuntime<R, E> extends Runtime<Kind<Kind<ZIO_, R>, E>> {
  
  static <R, E> ZIORuntime<R, E> instance(R env) {
    return () -> env;
  }

  R env();

  @Override
  default <T> T run(Kind<Kind<Kind<ZIO_, R>, E>, T> value) {
    return value.fix(toZIO()).provide(env()).getRight();
  }
  
  @Override
  default <T> Sequence<T> run(Sequence<Kind<Kind<Kind<ZIO_, R>, E>, T>> values) {
    return run(ZIO.traverse(values.map(ZIOOf::<R, E, T>narrowK)));
  }

  @Override
  default <T> Future<T> parRun(Kind<Kind<Kind<ZIO_, R>, E>, T> value, Executor executor) {
    return value.fix(toZIO()).runAsync(env()).map(Either::get);
  }
  
  @Override
  default <T> Future<Sequence<T>> parRun(Sequence<Kind<Kind<Kind<ZIO_, R>, E>, T>> values, Executor executor) {
    return parRun(ZIO.traverse(values.map(ZIOOf::<R, E, T>narrowK)), executor);
  }
}