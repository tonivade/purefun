/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.transformer;

import static com.github.tonivade.purefun.core.Unit.unit;

import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Witness;
import com.github.tonivade.purefun.core.Bindable;
import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Function2;
import com.github.tonivade.purefun.core.Operator1;
import com.github.tonivade.purefun.core.Tuple2;
import com.github.tonivade.purefun.core.Unit;
import com.github.tonivade.purefun.data.ImmutableList;
import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.typeclasses.FunctionK;
import com.github.tonivade.purefun.typeclasses.Monad;

@HigherKind
public non-sealed interface StateT<F extends Witness, S, A> extends StateTOf<F, S, A>, Bindable<Kind<Kind<StateT_, F>, S>, A> {

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
  default <R> StateT<F, S, R> flatMap(Function1<? super A, ? extends Kind<Kind<Kind<StateT_, F>, S>, ? extends R>> map) {
    return state(monad(), state -> {
      Kind<F, Tuple2<S, A>> newState = run(state);
      return monad().flatMap(newState, state2 -> map.andThen(StateTOf::<F, S, R>narrowK).apply(state2.get2()).run(state2.get1()));
    });
  }

  default <G extends Witness> StateT<G, S, A> mapK(Monad<G> other, FunctionK<F, G> functionK) {
    return state(other, state -> functionK.apply(run(state)));
  }

  static <F extends Witness, S, A> StateT<F, S, A> state(Monad<F> monad, Function1<S, Kind<F, Tuple2<S, A>>> run) {
    return new StateT<>() {

      @Override
      public Monad<F> monad() { return monad; }

      @Override
      public Kind<F, Tuple2<S, A>> run(S state) { return run.apply(state); }
    };
  }

  static <F extends Witness, S, A> StateT<F, S, A> lift(Monad<F> monad, Function1<S, Tuple2<S, A>> run) {
    return state(monad, run.andThen(monad::<Tuple2<S, A>>pure));
  }

  static <F extends Witness, S, A> StateT<F, S, A> pure(Monad<F> monad, A value) {
    return lift(monad, state -> Tuple2.of(state, value));
  }

  static <F extends Witness, S> StateT<F, S, S> get(Monad<F> monad) {
    return lift(monad, state -> Tuple2.of(state, state));
  }

  static <F extends Witness, S> StateT<F, S, Unit> set(Monad<F> monad, S value) {
    return lift(monad, state -> Tuple2.of(value, unit()));
  }

  static <F extends Witness, S> StateT<F, S, Unit> modify(Monad<F> monad, Operator1<S> mapper) {
    return lift(monad, state -> Tuple2.of(mapper.apply(state), unit()));
  }

  static <F extends Witness, S, A> StateT<F, S, A> inspect(Monad<F> monad, Function1<S, A> mapper) {
    return lift(monad, state -> Tuple2.of(state, mapper.apply(state)));
  }

  static <F extends Witness, S, A> StateT<F, S, Sequence<A>> traverse(Monad<F> monad,
                                                                      Sequence<StateT<F, S, A>> states) {
    return states.foldLeft(pure(monad, ImmutableList.empty()), 
        (StateT<F, S, Sequence<A>> xs, StateT<F, S, A> a) -> map2(xs, a, Sequence::append));
  }

  static <F extends Witness, S, A, B, C> StateT<F, S, C> map2(StateT<F, S, ? extends A> sa,
                                                              StateT<F, S, ? extends B> sb,
                                                              Function2<? super A, ? super B, ? extends C> mapper) {
    return sa.flatMap(a -> sb.map(b -> mapper.curried().apply(a).apply(b)));
  }

  static <F extends Witness, S, A> StateT<F, S, A> of(Monad<F> monad, Function1<S, Kind<F, Tuple2<S, A>>> run) {
    return state(monad, run);
  }
}
