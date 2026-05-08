/*
 * Copyright (c) 2018-2026, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.transformer;

import static com.github.tonivade.purefun.core.Unit.unit;

import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.core.Bindable;
import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Function2;
import com.github.tonivade.purefun.core.Operator1;
import com.github.tonivade.purefun.core.Tuple;
import com.github.tonivade.purefun.core.Tuple2;
import com.github.tonivade.purefun.core.Unit;
import com.github.tonivade.purefun.data.ImmutableList;
import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.typeclasses.FunctionK;
import com.github.tonivade.purefun.typeclasses.Instances;
import com.github.tonivade.purefun.typeclasses.Monad;

@HigherKind
public non-sealed interface StateT<F extends Kind<F, ?>, S, A> extends StateTOf<F, S, A>, Bindable<StateT<F, S, ?>, A> {

  Monad<F> monad();

  Kind<F, Tuple2<S, A>> run(S state);

  default Kind<F, A> eval(S state) {
    return monad().map(run(state), Tuple2::get2);
  }

  @Override
  default <R> StateT<F, S, R> map(Function1<? super A, ? extends R> map) {
    return flatMap(value -> pure(monad(), map.apply(value)));
  }

  @Override
  default <R> StateT<F, S, R> flatMap(Function1<? super A, ? extends Kind<StateT<F, S, ?>, ? extends R>> map) {
    return state(monad(), state -> {
      Kind<F, Tuple2<S, A>> newState = run(state);
      return monad().flatMap(newState, state2 -> map.andThen(StateTOf::<F, S, R>toStateT).apply(state2.get2()).run(state2.get1()));
    });
  }

  @Override
  default <R> StateT<F, S, R> andThen(Kind<StateT<F, S, ?>, ? extends R> next) {
    return flatMap(ignore -> next);
  }

  default <G extends Kind<G, ?>> StateT<G, S, A> mapK(Monad<G> other, FunctionK<F, G> functionK) {
    return state(other, state -> functionK.apply(run(state)));
  }

  @SuppressWarnings("unchecked")
  default <G extends Kind<G, ?>> StateT<G, S, A> mapK(FunctionK<F, G> functionK, G...reified) {
    return mapK(Instances.monad(reified), functionK);
  }

  static <F extends Kind<F, ?>, S, A> StateT<F, S, A> state(Monad<F> monad, Function1<S, Kind<F, Tuple2<S, A>>> run) {
    return new StateT<>() {

      @Override
      public Monad<F> monad() {
        return monad;
      }

      @Override
      public Kind<F, Tuple2<S, A>> run(S state) {
        return run.apply(state);
      }
    };
  }

  @SuppressWarnings("unchecked")
  static <F extends Kind<F, ?>, S, A> StateT<F, S, A> state(Function1<S, Kind<F, Tuple2<S, A>>> run, F...reified) {
    return state(Instances.monad(reified), run);
  }

  static <F extends Kind<F, ?>, S, A> StateT<F, S, A> lift(Monad<F> monad, Kind<F, A> value) {
    return state(monad, state -> monad.map(value, a -> Tuple.<S, A>of(state, a)));
  }

  @SafeVarargs
  static <F extends Kind<F, ?>, S, A> StateT<F, S, A> lift(Kind<F, A> value, F...reified) {
    return lift(Instances.monad(reified), value);
  }

  static <F extends Kind<F, ?>, S, A> StateT<F, S, A> lift(Monad<F> monad, Function1<S, Tuple2<S, A>> run) {
    return state(monad, run.andThen(monad::<Tuple2<S, A>>pure));
  }

  @SuppressWarnings("unchecked")
  static <F extends Kind<F, ?>, S, A> StateT<F, S, A> lift(Function1<S, Tuple2<S, A>> run, F...reified) {
    return lift(Instances.monad(reified), run);
  }

  static <F extends Kind<F, ?>, S, A> StateT<F, S, A> pure(Monad<F> monad, A value) {
    return lift(monad, state -> Tuple2.of(state, value));
  }

  @SafeVarargs
  static <F extends Kind<F, ?>, S, A> StateT<F, S, A> pure(A value, F...reified) {
    return pure(Instances.monad(reified), value);
  }

  static <F extends Kind<F, ?>, S> StateT<F, S, S> get(Monad<F> monad) {
    return lift(monad, state -> Tuple2.of(state, state));
  }

  @SuppressWarnings("unchecked")
  static <F extends Kind<F, ?>, S> StateT<F, S, S> get(F...reified) {
    return get(Instances.monad(reified));
  }

  static <F extends Kind<F, ?>, S> StateT<F, S, Unit> set(Monad<F> monad, S value) {
    return lift(monad, state -> Tuple2.of(value, unit()));
  }

  @SuppressWarnings("unchecked")
  static <F extends Kind<F, ?>, S> StateT<F, S, Unit> set(S value, F...reified) {
    return set(Instances.monad(reified), value);
  }

  static <F extends Kind<F, ?>, S> StateT<F, S, Unit> modify(Monad<F> monad, Operator1<S> mapper) {
    return lift(monad, state -> Tuple2.of(mapper.apply(state), unit()));
  }

  @SuppressWarnings("unchecked")
  static <F extends Kind<F, ?>, S> StateT<F, S, Unit> modify(Operator1<S> mapper, F...reified) {
    return modify(Instances.monad(reified), mapper);
  }

  static <F extends Kind<F, ?>, S, A> StateT<F, S, A> inspect(Monad<F> monad, Function1<S, A> mapper) {
    return lift(monad, state -> Tuple2.of(state, mapper.apply(state)));
  }

  @SuppressWarnings("unchecked")
  static <F extends Kind<F, ?>, S, A> StateT<F, S, A> inspect(Function1<S, A> mapper, F...reified) {
    return inspect(Instances.monad(reified), mapper);
  }

  static <F extends Kind<F, ?>, S, A> StateT<F, S, Sequence<A>> traverse(Monad<F> monad,
      Sequence<StateT<F, S, A>> states) {
    return states.foldLeft(pure(monad, ImmutableList.empty()),
        (StateT<F, S, Sequence<A>> xs, StateT<F, S, A> a) -> map2(xs, a, Sequence::append));
  }

  @SuppressWarnings("unchecked")
  static <F extends Kind<F, ?>, S, A> StateT<F, S, Sequence<A>> traverse(
      Sequence<StateT<F, S, A>> states, F...reified) {
    return traverse(Instances.monad(reified), states);
  }

  static <F extends Kind<F, ?>, S, A, B, C> StateT<F, S, C> map2(StateT<F, S, ? extends A> sa,
      StateT<F, S, ? extends B> sb, Function2<? super A, ? super B, ? extends C> mapper) {
    return sa.flatMap(a -> sb.map(b -> mapper.curried().apply(a).apply(b)));
  }

  static <F extends Kind<F, ?>, S, A> StateT<F, S, A> of(
      Monad<F> monad, Function1<S, Kind<F, Tuple2<S, A>>> run) {
    return state(monad, run);
  }

  @SuppressWarnings("unchecked")
  static <F extends Kind<F, ?>, S, A> StateT<F, S, A> of(
      Function1<S, Kind<F, Tuple2<S, A>>> run, F...reified) {
    return of(Instances.monad(reified), run);
  }
}
