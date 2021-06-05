/*
 * Copyright (c) 2018-2021, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import static com.github.tonivade.purefun.Function1.identity;

import java.time.Duration;

import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.concurrent.Future;
import com.github.tonivade.purefun.concurrent.Future_;
import com.github.tonivade.purefun.concurrent.Par;
import com.github.tonivade.purefun.concurrent.ParOf;
import com.github.tonivade.purefun.concurrent.Par_;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Try;
import com.github.tonivade.purefun.typeclasses.Applicative;
import com.github.tonivade.purefun.typeclasses.Async;
import com.github.tonivade.purefun.typeclasses.Bracket;
import com.github.tonivade.purefun.typeclasses.Concurrent;
import com.github.tonivade.purefun.typeclasses.Defer;
import com.github.tonivade.purefun.typeclasses.Fiber;
import com.github.tonivade.purefun.typeclasses.Functor;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.MonadDefer;
import com.github.tonivade.purefun.typeclasses.MonadThrow;
import com.github.tonivade.purefun.typeclasses.Reference;
import com.github.tonivade.purefun.typeclasses.Resource;

public interface ParInstances {

  static Functor<Par_> functor() {
    return ParFunctor.INSTANCE;
  }

  static Applicative<Par_> applicative() {
    return PureApplicative.INSTANCE;
  }

  static Monad<Par_> monad() {
    return ParMonad.INSTANCE;
  }

  static MonadDefer<Par_> monadDefer() {
    return ParMonadDefer.INSTANCE;
  }

  static Async<Par_> async() {
    return ParAsync.INSTANCE;
  }
  
  static Concurrent<Par_> concurrent() {
    return ParConcurrent.INSTANCE;
  }
  
  static <A> Reference<Par_, A> reference(A value) {
    return Reference.of(monadDefer(), value);
  }
  
  static <A extends AutoCloseable> Resource<Par_, A> resource(Par<A> acquire) {
    return resource(acquire, AutoCloseable::close);
  }
  
  static <A> Resource<Par_, A> resource(Par<A> acquire, Consumer1<A> release) {
    return Resource.from(monadDefer(), acquire, release);
  }
}

interface ParFunctor extends Functor<Par_> {

  ParFunctor INSTANCE = new ParFunctor() {};

  @Override
  default <T, R> Par<R> map(Kind<Par_, ? extends T> value, Function1<? super T, ? extends R> mapper) {
    return value.fix(ParOf::narrowK).map(mapper);
  }
}

interface ParPure extends Applicative<Par_> {
  @Override
  default <T> Par<T> pure(T value) {
    return Par.success(value);
  }
}

interface PureApplicative extends ParPure {

  PureApplicative INSTANCE = new PureApplicative() {};

  @Override
  default <T, R> Par<R> ap(Kind<Par_, ? extends T> value, 
      Kind<Par_, ? extends Function1<? super T, ? extends R>> apply) {
    return value.fix(ParOf::<T>narrowK).ap(apply.fix(ParOf::narrowK));
  }
}

interface ParMonad extends ParPure, Monad<Par_> {

  ParMonad INSTANCE = new ParMonad() {};

  @Override
  default <T, R> Par<R> flatMap(Kind<Par_, ? extends T> value, Function1<? super T, ? extends Kind<Par_, ? extends R>> map) {
    return value.fix(ParOf::narrowK).flatMap(x -> map.apply(x).fix(ParOf::narrowK));
  }

  /**
   * XXX In order to create real parallel computations, we need to override ap to use the
   * applicative version of the ap method
   */
  @Override
  default <T, R> Par<R> ap(Kind<Par_, ? extends T> value, 
      Kind<Par_, ? extends Function1<? super T, ? extends R>> apply) {
    return ParInstances.applicative().ap(value, apply).fix(ParOf::narrowK);
  }
}

interface ParMonadThrow extends ParMonad, MonadThrow<Par_> {

  ParMonadThrow INSTANCE = new ParMonadThrow() {};

  @Override
  default <A> Par<A> raiseError(Throwable error) {
    return Par.<A>failure(error);
  }

  @Override
  default <A> Par<A> handleErrorWith(Kind<Par_, A> value,
                                     Function1<? super Throwable, ? extends Kind<Par_, ? extends A>> handler) {
    return ParOf.narrowK(value).fold(handler.andThen(ParOf::narrowK), Par::success).flatMap(identity());
  }
}

interface ParDefer extends Defer<Par_> {

  @Override
  default <A> Par<A> defer(Producer<? extends Kind<Par_, ? extends A>> defer) {
    return Par.defer(defer.map(ParOf::<A>narrowK)::get);
  }
}

interface ParBracket extends Bracket<Par_, Throwable> {

  @Override
  default <A, B> Par<B> bracket(
      Kind<Par_, ? extends A> acquire, 
      Function1<? super A, ? extends Kind<Par_, ? extends B>> use, 
      Consumer1<? super A> release) {
    return Par.bracket(ParOf.narrowK(acquire), use.andThen(ParOf::narrowK), release);
  }
}

interface ParMonadDefer extends ParMonadThrow, ParDefer, ParBracket, MonadDefer<Par_> {

  ParMonadDefer INSTANCE = new ParMonadDefer() {};

  @Override
  default Par<Unit> sleep(Duration duration) {
    return Par.sleep(duration);
  }
}

interface ParAsync extends Async<Par_>, ParMonadDefer {
  
  ParAsync INSTANCE = new ParAsync() {};
  
  @Override
  default <A> Par<A> asyncF(Function1<Consumer1<? super Try<? extends A>>, Kind<Par_, Unit>> consumer) {
    return Par.asyncF(consumer.andThen(ParOf::narrowK));
  }
}

interface ParConcurrent extends Concurrent<Par_>, ParAsync {
  
  ParConcurrent INSTANCE = new ParConcurrent() {};

  @Override
  default <A> Par<Fiber<Par_, A>> fork(Kind<Par_, A> value) {
    return Par.later(() -> fiber(value.fix(ParOf.toPar())));
  }
  
  @Override
  default <A, B> Par<Either<Tuple2<A, Fiber<Par_, B>>, Tuple2<Fiber<Par_, A>, B>>> racePair(
      Kind<Par_, A> fa, Kind<Par_, B> fb) {

    return executor -> {
      Future<Either<Tuple2<A, Fiber<Future_, B>>, Tuple2<Fiber<Future_, A>, B>>> racePair = FutureConcurrent.instance(executor).racePair(
          fa.fix(ParOf.toPar()).apply(executor), 
          fb.fix(ParOf.toPar()).apply(executor));
      
      return racePair.map(either -> either.bimap(t1 -> t1.map2(b -> fiber(fb)), t2 -> t2.map1(a -> fiber(fa))));
    };
  }
  
  static <A> Fiber<Par_, A> fiber(Kind<Par_, A> par) {
    return Fiber.of(() -> par, () -> Par.unit());
  }
}
