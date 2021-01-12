/*
 * Copyright (c) 2018-2021, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.Function2;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Witness;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.Matcher1;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Unit;

public interface MonadError<F extends Witness, E> extends ApplicativeError<F, E>, Monad<F> {

  default <A> Kind<F, A> ensure(Kind<F, A> value, Producer<E> error, Matcher1<A> matcher) {
    return flatMap(value, a -> matcher.match(a) ? pure(a) : raiseError(error.get()));
  }
  
  default <A, B> Kind<F, B> repeat(Kind<F, A> value, Schedule<F, A, B> schedule) {
    return repeatOrElse(value, schedule, (e, b) -> raiseError(e));
  }

  default <A, B> Kind<F, B> repeatOrElse(Kind<F, A> value, Schedule<F, A, B> schedule, Function2<E, Option<B>, Kind<F, B>> orElse) {
    return map(repeatOrElseEither(value, schedule, orElse), Either::merge);
  }

  @SuppressWarnings("unchecked")
  default <A, B, C> Kind<F, Either<C, B>> repeatOrElseEither(
      Kind<F, A> value, Schedule<F, A, B> schedule, Function2<E, Option<B>, Kind<F, C>> orElse) {
    return MonadErrorModule.repeat(this, value, (ScheduleImpl<F, ?, A, B>) schedule, orElse);
  }
  
  default <A, B> Kind<F, A> retry(Kind<F, A> value, Schedule<F, E, B> schedule) {
    return retryOrElse(value, schedule, (e, b) -> raiseError(e));
  }

  default <A, B> Kind<F, A> retryOrElse(Kind<F, A> value, Schedule<F, E, B> schedule, Function2<E, B, Kind<F, A>> orElse) {
    return map(retryOrElseEither(value, schedule, orElse), Either::merge);
  }

  @SuppressWarnings("unchecked")
  default <A, B, C> Kind<F, Either<B, A>> retryOrElseEither(
      Kind<F, A> value, Schedule<F, E, C> schedule, Function2<E, C, Kind<F, B>> orElse) {
    return MonadErrorModule.retry(this, value, (ScheduleImpl<F, C, E, C>) schedule, orElse);
  }
}

interface MonadErrorModule {

  static <F extends Witness, E, A, B, C, S> Kind<F, Either<C, B>> repeat(
      MonadError<F, E> monad, Kind<F, A> value, ScheduleImpl<F, S, A, B> schedule, Function2<E, Option<B>, Kind<F, C>> orElse) {
    
    class Loop {
      private Kind<F, Either<C, B>> loop(A last, S state) {
        return monad.flatMap(schedule.update(last, state), decision -> decision.fold(
            ignore -> monad.pure(Either.right(schedule.extract(last, state))), 
            s -> monad.flatMap(monad.attempt(value), either -> either.fold(
                e -> monad.map(orElse.apply(e, Option.some(schedule.extract(last, state))), Either::<C, B>left), 
                a -> loop(a, s)))));
      }
    }
    
    return monad.flatMap(monad.attempt(value), either -> either.fold(
        error -> monad.map(orElse.apply(error, Option.<B>none()), Either::<C, B>left), 
        a -> monad.flatMap(schedule.initial(), s -> new Loop().loop(a, s))));
  }

  static <F extends Witness, E, A, B, S> Kind<F, Either<B, A>> retry(
      MonadError<F, E> monad, Kind<F, A> value, ScheduleImpl<F, S, E, S> schedule, Function2<E, S, Kind<F, B>> orElse) {
    
    class Loop {
      private Kind<F, Either<B, A>> loop(S state) {
        return monad.flatMap(monad.attempt(value), either -> either.fold(
            error -> {
              Kind<F, Either<Unit, S>> update = schedule.update(error, state);
              return monad.flatMap(update, decision -> decision.fold(
                  ignore -> monad.map(orElse.apply(error, state), Either::<B, A>left), 
                  this::loop));
            }, 
            a -> monad.pure(Either.right(a)))
         );
      }
    }
    
    return monad.flatMap(schedule.initial(), new Loop()::loop);
  }
}
