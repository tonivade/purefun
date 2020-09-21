/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import static com.github.tonivade.purefun.Precondition.checkNonNull;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Tuple;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.Witness;
import com.github.tonivade.purefun.transformer.StateT;
import com.github.tonivade.purefun.transformer.StateTOf;
import com.github.tonivade.purefun.transformer.StateT_;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.MonadError;
import com.github.tonivade.purefun.typeclasses.MonadReader;
import com.github.tonivade.purefun.typeclasses.MonadState;

public interface StateTInstances {

  static <F extends Witness, S> Monad<Kind<Kind<StateT_, F>, S>> monad(Monad<F> monadF) {
    return StateTMonad.instance(checkNonNull(monadF));
  }

  static <F extends Witness, S> MonadState<Kind<Kind<StateT_, F>, S>, S> monadState(Monad<F> monadF) {
    return StateTMonadState.instance(checkNonNull(monadF));
  }

  static <F extends Witness, S, E> MonadError<Kind<Kind<StateT_, F>, S>, E> monadError(MonadError<F, E> monadErrorF) {
    return StateTMonadError.instance(checkNonNull(monadErrorF));
  }

  static <F extends Witness, S, R> MonadReader<Kind<Kind<StateT_, F>, S>, R> monadReader(MonadReader<F, R> monadReaderF) {
    return StateTMonadReader.instance(checkNonNull(monadReaderF));
  }
}

interface StateTMonad<F extends Witness, S> extends Monad<Kind<Kind<StateT_, F>, S>> {

  static <F extends Witness, S> StateTMonad<F, S> instance(Monad<F> monadF) {
    return () -> monadF;
  }

  Monad<F> monadF();

  @Override
  default <T> StateT<F, S, T> pure(T value) {
    return StateT.<F, S, T>pure(monadF(), value);
  }

  @Override
  default <T, R> StateT<F, S, R> flatMap(Kind<Kind<Kind<StateT_, F>, S>, T> value,
      Function1<? super T, ? extends Kind<Kind<Kind<StateT_, F>, S>, ? extends R>> map) {
    return StateTOf.narrowK(value).flatMap(map.andThen(StateTOf::narrowK));
  }
}

interface StateTMonadError<F extends Witness, S, E> extends MonadError<Kind<Kind<StateT_, F>, S>, E>, StateTMonad<F, S> {

  static <F extends Witness, S, E> StateTMonadError<F, S, E> instance(MonadError<F, E> monadErrorF) {
    return () -> monadErrorF;
  }

  @Override
  MonadError<F, E> monadF();

  @Override
  default <A> StateT<F, S, A> raiseError(E error) {
    Kind<F, A> raiseError = monadF().raiseError(error);
    return StateT.<F, S, A>state(monadF(), state -> monadF().map(raiseError, value -> Tuple.of(state, value)));
  }

  @Override
  default <A> StateT<F, S, A> handleErrorWith(
      Kind<Kind<Kind<StateT_, F>, S>, A> value,
      Function1<E, ? extends Kind<Kind<Kind<StateT_, F>, S>, A>> handler) {
    StateT<F, S, A> stateT = value.fix(StateTOf::narrowK);
    return StateT.<F, S, A>state(monadF(),
        state -> monadF().handleErrorWith(stateT.run(state),
            error -> handler.apply(error).fix(StateTOf::narrowK).run(state)));
  }
}

interface StateTMonadState<F extends Witness, S> extends MonadState<Kind<Kind<StateT_, F>, S>, S>, StateTMonad<F, S> {

  static <F extends Witness, S> StateTMonadState<F, S> instance(Monad<F> monadF) {
    return () -> monadF;
  }

  @Override
  default StateT<F, S, S> get() {
    return StateT.<F, S>get(monadF());
  }

  @Override
  default StateT<F, S, Unit> set(S state) {
    return StateT.set(monadF(), state);
  }
}

interface StateTMonadReader<F extends Witness, S, R> extends MonadReader<Kind<Kind<StateT_, F>, S>, R>, StateTMonad<F, S> {

  static <F extends Witness, S, R> StateTMonadReader<F, S, R> instance(MonadReader<F, R> monadReaderF) {
    return () -> monadReaderF;
  }

  @Override
  MonadReader<F, R> monadF();

  @Override
  default StateT<F, S, R> ask() {
    return StateT.<F, S, R>state(monadF(), state -> monadF().map(monadF().ask(), reader -> Tuple.of(state, reader)));
  }
}
