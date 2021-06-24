/*
 * Copyright (c) 2018-2021, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import static com.github.tonivade.purefun.effect.EIOOf.toEIO;
import java.time.Duration;
import java.util.concurrent.Executor;
import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.concurrent.Future;
import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.effect.EIO;
import com.github.tonivade.purefun.effect.EIOOf;
import com.github.tonivade.purefun.effect.EIO_;
import com.github.tonivade.purefun.effect.UIO;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Try;
import com.github.tonivade.purefun.typeclasses.Applicative;
import com.github.tonivade.purefun.typeclasses.Async;
import com.github.tonivade.purefun.typeclasses.Bracket;
import com.github.tonivade.purefun.typeclasses.Defer;
import com.github.tonivade.purefun.typeclasses.Functor;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.MonadDefer;
import com.github.tonivade.purefun.typeclasses.MonadError;
import com.github.tonivade.purefun.typeclasses.MonadThrow;
import com.github.tonivade.purefun.typeclasses.Runtime;

@SuppressWarnings("unchecked")
public interface EIOInstances {

  static <E> Functor<Kind<EIO_, E>> functor() {
    return EIOFunctor.INSTANCE;
  }

  static <E> Applicative<Kind<EIO_, E>> applicative() {
    return EIOApplicative.INSTANCE;
  }

  static <E> Monad<Kind<EIO_, E>> monad() {
    return EIOMonad.INSTANCE;
  }

  static <E> MonadError<Kind<EIO_, E>, E> monadError() {
    return EIOMonadError.INSTANCE;
  }

  static MonadThrow<Kind<EIO_, Throwable>> monadThrow() {
    return EIOMonadThrow.INSTANCE;
  }

  static MonadDefer<Kind<EIO_, Throwable>> monadDefer() {
    return EIOMonadDefer.INSTANCE;
  }

  static Async<Kind<EIO_, Throwable>> async() {
    return EIOAsync.INSTANCE;
  }
  
  static <E> Runtime<Kind<EIO_, E>> runtime() {
    return EIORuntime.INSTANCE;
  }
}

interface EIOFunctor<E> extends Functor<Kind<EIO_, E>> {

  @SuppressWarnings("rawtypes")
  EIOFunctor INSTANCE = new EIOFunctor() {};

  @Override
  default <A, B> EIO<E, B>
          map(Kind<Kind<EIO_, E>, ? extends A> value, Function1<? super A, ? extends B> map) {
    return EIOOf.narrowK(value).map(map);
  }
}

interface EIOPure<E> extends Applicative<Kind<EIO_, E>> {

  @Override
  default <A> EIO<E, A> pure(A value) {
    return EIO.<E, A>pure(value);
  }
}

interface EIOApplicative<E> extends EIOPure<E> {

  @SuppressWarnings("rawtypes")
  EIOApplicative INSTANCE = new EIOApplicative() {};

  @Override
  default <A, B> EIO<E, B>
          ap(Kind<Kind<EIO_, E>, ? extends A> value,
             Kind<Kind<EIO_, E>, ? extends Function1<? super A, ? extends B>> apply) {
    return value.fix(EIOOf::<E, A>narrowK).ap(apply.fix(EIOOf::narrowK));
  }
}

interface EIOMonad<E> extends EIOPure<E>, Monad<Kind<EIO_, E>> {

  @SuppressWarnings("rawtypes")
  EIOMonad INSTANCE = new EIOMonad() {};

  @Override
  default <A, B> EIO<E, B>
          flatMap(Kind<Kind<EIO_, E>, ? extends A> value,
                  Function1<? super A, ? extends Kind<Kind<EIO_, E>, ? extends B>> map) {
    return value.fix(toEIO()).flatMap(map.andThen(EIOOf::narrowK));
  }
}

interface EIOMonadError<E> extends EIOMonad<E>, MonadError<Kind<EIO_, E>, E> {

  @SuppressWarnings("rawtypes")
  EIOMonadError INSTANCE = new EIOMonadError() {};

  @Override
  default <A> EIO<E, A> raiseError(E error) {
    return EIO.<E, A>raiseError(error);
  }

  @Override
  default <A> EIO<E, A> handleErrorWith(
      Kind<Kind<EIO_,  E>, A> value,
      Function1<? super E, ? extends Kind<Kind<EIO_, E>, ? extends A>> handler) {
    // XXX: java8 fails to infer types, I have to do this in steps
    Function1<? super E, EIO<E, A>> mapError = handler.andThen(EIOOf::narrowK);
    Function1<A, EIO<E, A>> map = EIO::pure;
    EIO<E, A> eio = EIOOf.narrowK(value);
    return eio.foldM(mapError, map);
  }
}

interface EIOMonadThrow
    extends EIOMonadError<Throwable>,
            MonadThrow<Kind<EIO_, Throwable>> {

  EIOMonadThrow INSTANCE = new EIOMonadThrow() {};
}

interface EIODefer<E> extends Defer<Kind<EIO_, E>> {

  @Override
  default <A> EIO<E, A>
          defer(Producer<? extends Kind<Kind<EIO_, E>, ? extends A>> defer) {
    return EIO.defer(() -> defer.map(EIOOf::<E, A>narrowK).get());
  }
}

interface EIOBracket<E> extends EIOMonadError<E>, Bracket<Kind<EIO_, E>, E> {

  @Override
  default <A, B> EIO<E, B>
          bracket(Kind<Kind<EIO_, E>, ? extends A> acquire,
                  Function1<? super A, ? extends Kind<Kind<EIO_, E>, ? extends B>> use,
                  Function1<? super A, ? extends Kind<Kind<EIO_, E>, Unit>> release) {
    return EIO.bracket(acquire.fix(toEIO()), use.andThen(EIOOf::narrowK), release::apply);
  }
}

interface EIOMonadDefer
    extends MonadDefer<Kind<EIO_, Throwable>>, EIODefer<Throwable>, EIOBracket<Throwable> {

  EIOMonadDefer INSTANCE = new EIOMonadDefer() {};

  @Override
  default EIO<Throwable, Unit> sleep(Duration duration) {
    return UIO.sleep(duration).<Throwable>toEIO();
  }
}

interface EIOAsync extends Async<Kind<EIO_, Throwable>>, EIOMonadDefer {

  EIOAsync INSTANCE = new EIOAsync() {};
  
  @Override
  default <A> EIO<Throwable, A> asyncF(Function1<Consumer1<? super Try<? extends A>>, Kind<Kind<EIO_, Throwable>, Unit>> consumer) {
    return EIO.cancellable(cb -> consumer.andThen(EIOOf::narrowK).apply(e -> cb.accept(e.toEither())));
  }
}

interface EIORuntime<E> extends Runtime<Kind<EIO_, E>> {
  
  @SuppressWarnings("rawtypes")
  EIORuntime INSTANCE = new EIORuntime() {};

  @Override
  default <T> T run(Kind<Kind<EIO_, E>, T> value) {
    return value.fix(toEIO()).safeRunSync().getRight();
  }
  
  @Override
  default <T> Sequence<T> run(Sequence<Kind<Kind<EIO_, E>, T>> values) {
    return run(EIO.traverse(values.map(EIOOf::<E, T>narrowK)));
  }

  @Override
  default <T> Future<T> parRun(Kind<Kind<EIO_, E>, T> value, Executor executor) {
    return value.fix(toEIO()).runAsync().map(Either::get);
  }
  
  @Override
  default <T> Future<Sequence<T>> parRun(Sequence<Kind<Kind<EIO_, E>, T>> values, Executor executor) {
    return parRun(EIO.traverse(values.map(EIOOf::<E, T>narrowK)), executor);
  }
}