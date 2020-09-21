/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import static com.github.tonivade.purefun.Function1.identity;
import java.time.Duration;
import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.concurrent.Par;
import com.github.tonivade.purefun.concurrent.ParOf;
import com.github.tonivade.purefun.concurrent.Par_;
import com.github.tonivade.purefun.type.Try;
import com.github.tonivade.purefun.typeclasses.Applicative;
import com.github.tonivade.purefun.typeclasses.Async;
import com.github.tonivade.purefun.typeclasses.Bracket;
import com.github.tonivade.purefun.typeclasses.Defer;
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
  default <T, R> Par<R> map(Kind<Par_, T> value, Function1<? super T, ? extends R> mapper) {
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
  default <T, R> Par<R> ap(Kind<Par_, T> value, Kind<Par_, Function1<? super T, ? extends R>> apply) {
    return value.fix(ParOf::narrowK).ap(apply.fix(ParOf::narrowK));
  }
}

interface ParMonad extends ParPure, Monad<Par_> {

  ParMonad INSTANCE = new ParMonad() {};

  @Override
  default <T, R> Par<R> flatMap(Kind<Par_, T> value, Function1<? super T, ? extends Kind<Par_, ? extends R>> map) {
    return value.fix(ParOf::narrowK).flatMap(x -> map.apply(x).fix(ParOf::narrowK));
  }

  /**
   * XXX In order to create real parallel computations, we need to override ap to use the
   * applicative version of the ap method
   */
  @Override
  default <T, R> Par<R> ap(Kind<Par_, T> value, Kind<Par_, Function1<? super T, ? extends R>> apply) {
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
                                                Function1<Throwable, ? extends Kind<Par_, A>> handler) {
    return ParOf.narrowK(value).fold(handler.andThen(ParOf::narrowK), Par::success).flatMap(identity());
  }
}

interface ParDefer extends Defer<Par_> {

  @Override
  default <A> Par<A> defer(Producer<Kind<Par_, A>> defer) {
    return Par.defer(defer.map(ParOf::narrowK)::get);
  }
}

interface ParBracket extends Bracket<Par_, Throwable> {

  @Override
  default <A, B> Par<B> bracket(
      Kind<Par_, A> acquire, Function1<A, ? extends Kind<Par_, B>> use, Consumer1<A> release) {
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
  default <A> Kind<Par_, A> async(Consumer1<Consumer1<Try<A>>> consumer) {
    return Par.async(consumer);
  }
}
