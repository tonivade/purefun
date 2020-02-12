/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Instance;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.concurrent.Par;
import com.github.tonivade.purefun.typeclasses.Applicative;
import com.github.tonivade.purefun.typeclasses.Bracket;
import com.github.tonivade.purefun.typeclasses.Defer;
import com.github.tonivade.purefun.typeclasses.Functor;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.MonadDefer;
import com.github.tonivade.purefun.typeclasses.MonadThrow;

import java.time.Duration;

import static com.github.tonivade.purefun.Function1.identity;

public interface ParInstances {

  static Functor<Par.µ> functor() {
    return ParFunctor.instance();
  }

  static Applicative<Par.µ> applicative() {
    return PureApplicative.instance();
  }

  static Monad<Par.µ> monad() {
    return ParMonad.instance();
  }

  static MonadDefer<Par.µ> monadDefer() {
    return ParMonadDefer.instance();
  }
}

@Instance
interface ParFunctor extends Functor<Par.µ> {
  @Override
  default <T, R> Higher1<Par.µ, R> map(Higher1<Par.µ, T> value, Function1<T, R> mapper) {
    return value.fix1(Par::narrowK).map(mapper).kind1();
  }
}

interface ParPure extends Applicative<Par.µ> {
  @Override
  default <T> Higher1<Par.µ, T> pure(T value) {
    return Par.success(value).kind1();
  }
}

@Instance
interface PureApplicative extends ParPure {
  @Override
  default <T, R> Higher1<Par.µ, R> ap(Higher1<Par.µ, T> value, Higher1<Par.µ, Function1<T, R>> apply) {
    return value.fix1(Par::narrowK).ap(apply.fix1(Par::narrowK)).kind1();
  }
}

@Instance
interface ParMonad extends ParPure, Monad<Par.µ> {
  @Override
  default <T, R> Higher1<Par.µ, R> flatMap(Higher1<Par.µ, T> value, Function1<T, ? extends Higher1<Par.µ, R>> map) {
    return value.fix1(Par::narrowK).flatMap(x -> map.apply(x).fix1(Par::narrowK)).kind1();
  }
}

interface ParMonadThrow extends ParMonad, MonadThrow<Par.µ> {

  @Override
  default <A> Higher1<Par.µ, A> raiseError(Throwable error) {
    return Par.<A>failure(error).kind1();
  }

  @Override
  default <A> Higher1<Par.µ, A> handleErrorWith(Higher1<Par.µ, A> value,
                                                Function1<Throwable, ? extends Higher1<Par.µ, A>> handler) {
    return Par.narrowK(value).fold(handler.andThen(Par::narrowK), Par::success).flatMap(identity()).kind1();
  }
}

interface ParDefer extends Defer<Par.µ> {

  @Override
  default <A> Higher1<Par.µ, A> defer(Producer<Higher1<Par.µ, A>> defer) {
    return Par.defer(defer.map(Par::narrowK)::get).kind1();
  }
}

interface ParBracket extends Bracket<Par.µ> {

  @Override
  default <A, B> Higher1<Par.µ, B> bracket(
      Higher1<Par.µ, A> acquire, Function1<A, ? extends Higher1<Par.µ, B>> use, Consumer1<A> release) {
    return Par.bracket(Par.narrowK(acquire), use.andThen(Par::narrowK), release).kind1();
  }
}

@Instance
interface ParMonadDefer extends ParMonadThrow, ParDefer, ParBracket, MonadDefer<Par.µ> {

  @Override
  default Higher1<Par.µ, Unit> sleep(Duration duration) {
    return Par.sleep(duration).kind1();
  }
}
