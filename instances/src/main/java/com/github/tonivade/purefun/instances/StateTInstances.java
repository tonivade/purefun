/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import static com.github.tonivade.purefun.Precondition.checkNonNull;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Higher3;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Tuple;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.transformer.StateT;
import com.github.tonivade.purefun.transformer.StateT_;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.MonadError;
import com.github.tonivade.purefun.typeclasses.MonadReader;
import com.github.tonivade.purefun.typeclasses.MonadState;

public interface StateTInstances {

  static <F extends Kind, S> Monad<Higher1<Higher1<StateT_, F>, S>> monad(Monad<F> monadF) {
    return StateTMonad.instance(checkNonNull(monadF));
  }

  static <F extends Kind, S> MonadState<Higher1<Higher1<StateT_, F>, S>, S> monadState(Monad<F> monadF) {
    return StateTMonadState.instance(checkNonNull(monadF));
  }

  static <F extends Kind, S, E> MonadError<Higher1<Higher1<StateT_, F>, S>, E> monadError(MonadError<F, E> monadErrorF) {
    return StateTMonadError.instance(checkNonNull(monadErrorF));
  }

  static <F extends Kind, S, R> MonadReader<Higher1<Higher1<StateT_, F>, S>, R> monadReader(MonadReader<F, R> monadReaderF) {
    return StateTMonadReader.instance(checkNonNull(monadReaderF));
  }
}

interface StateTMonad<F extends Kind, S> extends Monad<Higher1<Higher1<StateT_, F>, S>> {

  static <F extends Kind, S> StateTMonad<F, S> instance(Monad<F> monadF) {
    return () -> monadF;
  }

  Monad<F> monadF();

  @Override
  default <T> Higher3<StateT_, F, S, T> pure(T value) {
    return StateT.<F, S, T>pure(monadF(), value);
  }

  @Override
  default <T, R> Higher3<StateT_, F, S, R> flatMap(Higher1<Higher1<Higher1<StateT_, F>, S>, T> value,
      Function1<T, ? extends Higher1<Higher1<Higher1<StateT_, F>, S>, R>> map) {
    return StateT_.narrowK(value).flatMap(map.andThen(StateT_::narrowK));
  }
}

interface StateTMonadError<F extends Kind, S, E> extends MonadError<Higher1<Higher1<StateT_, F>, S>, E>, StateTMonad<F, S> {

  static <F extends Kind, S, E> StateTMonadError<F, S, E> instance(MonadError<F, E> monadErrorF) {
    return () -> monadErrorF;
  }

  @Override
  MonadError<F, E> monadF();

  @Override
  default <A> Higher3<StateT_, F, S, A> raiseError(E error) {
    Higher1<F, A> raiseError = monadF().raiseError(error);
    return StateT.<F, S, A>state(monadF(), state -> monadF().map(raiseError, value -> Tuple.of(state, value)));
  }

  @Override
  default <A> Higher3<StateT_, F, S, A> handleErrorWith(
      Higher1<Higher1<Higher1<StateT_, F>, S>, A> value,
      Function1<E, ? extends Higher1<Higher1<Higher1<StateT_, F>, S>, A>> handler) {
    StateT<F, S, A> stateT = value.fix1(StateT_::narrowK);
    return StateT.<F, S, A>state(monadF(),
        state -> monadF().handleErrorWith(stateT.run(state),
            error -> handler.apply(error).fix1(StateT_::narrowK).run(state)));
  }
}

interface StateTMonadState<F extends Kind, S> extends MonadState<Higher1<Higher1<StateT_, F>, S>, S>, StateTMonad<F, S> {

  static <F extends Kind, S> StateTMonadState<F, S> instance(Monad<F> monadF) {
    return () -> monadF;
  }

  @Override
  default Higher3<StateT_, F, S, S> get() {
    return StateT.<F, S>get(monadF());
  }

  @Override
  default Higher3<StateT_, F, S, Unit> set(S state) {
    return StateT.set(monadF(), state);
  }
}

interface StateTMonadReader<F extends Kind, S, R> extends MonadReader<Higher1<Higher1<StateT_, F>, S>, R>, StateTMonad<F, S> {

  static <F extends Kind, S, R> StateTMonadReader<F, S, R> instance(MonadReader<F, R> monadReaderF) {
    return () -> monadReaderF;
  }

  @Override
  MonadReader<F, R> monadF();

  @Override
  default Higher3<StateT_, F, S, R> ask() {
    return StateT.<F, S, R>state(monadF(), state -> monadF().map(monadF().ask(), reader -> Tuple.of(state, reader)));
  }
}
