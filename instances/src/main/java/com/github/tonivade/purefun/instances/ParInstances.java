/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import static com.github.tonivade.purefun.core.Function1.identity;

import java.time.Duration;

import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.concurrent.Par;
import com.github.tonivade.purefun.concurrent.ParOf;
import com.github.tonivade.purefun.core.Consumer1;
import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Producer;
import com.github.tonivade.purefun.core.Unit;
import com.github.tonivade.purefun.typeclasses.Applicative;
import com.github.tonivade.purefun.typeclasses.Bracket;
import com.github.tonivade.purefun.typeclasses.Defer;
import com.github.tonivade.purefun.typeclasses.Functor;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.MonadDefer;
import com.github.tonivade.purefun.typeclasses.MonadThrow;
import com.github.tonivade.purefun.typeclasses.Reference;
import com.github.tonivade.purefun.typeclasses.Resource;

public interface ParInstances {

  static Functor<Par<?>> functor() {
    return ParFunctor.INSTANCE;
  }

  static Applicative<Par<?>> applicative() {
    return PureApplicative.INSTANCE;
  }

  static Monad<Par<?>> monad() {
    return ParMonad.INSTANCE;
  }

  static MonadDefer<Par<?>> monadDefer() {
    return ParMonadDefer.INSTANCE;
  }

  static <A> Reference<Par<?>, A> reference(A value) {
    return Reference.of(monadDefer(), value);
  }

  static <A extends AutoCloseable> Resource<Par<?>, A> resource(Par<A> acquire) {
    return resource(acquire, AutoCloseable::close);
  }

  static <A> Resource<Par<?>, A> resource(Par<A> acquire, Consumer1<A> release) {
    return Resource.from(monadDefer(), acquire, release);
  }
}

interface ParFunctor extends Functor<Par<?>> {

  ParFunctor INSTANCE = new ParFunctor() {};

  @Override
  default <T, R> Par<R> map(Kind<Par<?>, ? extends T> value, Function1<? super T, ? extends R> mapper) {
    return value.fix(ParOf::toPar).map(mapper);
  }
}

interface ParPure extends Applicative<Par<?>> {
  @Override
  default <T> Par<T> pure(T value) {
    return Par.success(value);
  }
}

interface PureApplicative extends ParPure {

  PureApplicative INSTANCE = new PureApplicative() {};

  @Override
  default <T, R> Par<R> ap(Kind<Par<?>, ? extends T> value,
      Kind<Par<?>, ? extends Function1<? super T, ? extends R>> apply) {
    return value.fix(ParOf::<T>toPar).ap(apply.fix(ParOf::toPar));
  }
}

interface ParMonad extends ParPure, Monad<Par<?>> {

  ParMonad INSTANCE = new ParMonad() {};

  @Override
  default <T, R> Par<R> flatMap(Kind<Par<?>, ? extends T> value, Function1<? super T, ? extends Kind<Par<?>, ? extends R>> map) {
    return value.fix(ParOf::toPar).flatMap(x -> map.apply(x).fix(ParOf::toPar));
  }

  /**
   * XXX In order to create real parallel computations, we need to override ap to use the
   * applicative version of the ap method
   */
  @Override
  default <T, R> Par<R> ap(Kind<Par<?>, ? extends T> value,
      Kind<Par<?>, ? extends Function1<? super T, ? extends R>> apply) {
    return ParInstances.applicative().ap(value, apply).fix(ParOf::toPar);
  }
}

interface ParMonadThrow extends ParMonad, MonadThrow<Par<?>> {

  ParMonadThrow INSTANCE = new ParMonadThrow() {};

  @Override
  default <A> Par<A> raiseError(Throwable error) {
    return Par.failure(error);
  }

  @Override
  default <A> Par<A> handleErrorWith(Kind<Par<?>, A> value,
                                     Function1<? super Throwable, ? extends Kind<Par<?>, ? extends A>> handler) {
    return ParOf.toPar(value).fold(handler.andThen(ParOf::toPar), Par::success).flatMap(identity());
  }
}

interface ParDefer extends Defer<Par<?>> {

  @Override
  default <A> Par<A> defer(Producer<? extends Kind<Par<?>, ? extends A>> defer) {
    return Par.defer(defer.map(ParOf::<A>toPar));
  }
}

interface ParBracket extends Bracket<Par<?>, Throwable> {

  @Override
  default <A, B> Par<B> bracket(
      Kind<Par<?>, ? extends A> acquire,
      Function1<? super A, ? extends Kind<Par<?>, ? extends B>> use,
      Function1<? super A, ? extends Kind<Par<?>, Unit>> release) {
    return Par.bracket(ParOf.toPar(acquire), use.andThen(ParOf::toPar), release::apply);
  }
}

interface ParMonadDefer extends ParMonadThrow, ParDefer, ParBracket, MonadDefer<Par<?>> {

  ParMonadDefer INSTANCE = new ParMonadDefer() {};

  @Override
  default Par<Unit> sleep(Duration duration) {
    return Par.sleep(duration);
  }
}
