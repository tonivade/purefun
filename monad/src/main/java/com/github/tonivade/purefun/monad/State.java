/*
 * Copyright (c) 2018-2025, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.monad;

import static com.github.tonivade.purefun.core.Unit.unit;
import static com.github.tonivade.purefun.data.ImmutableList.empty;

import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.core.Bindable;
import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Function2;
import com.github.tonivade.purefun.core.Operator1;
import com.github.tonivade.purefun.core.Tuple2;
import com.github.tonivade.purefun.core.Unit;
import com.github.tonivade.purefun.data.Sequence;

@HigherKind
@FunctionalInterface
public non-sealed interface State<S, A> extends StateOf<S, A>, Bindable<State<S, ?>, A> {

  Tuple2<S, A> run(S state);

  @Override
  default <R> State<S, R> map(Function1<? super A, ? extends R> mapper) {
    return flatMap(value -> pure(mapper.apply(value)));
  }

  @Override
  default <R> State<S, R> flatMap(Function1<? super A, ? extends Kind<State<S, ?>, ? extends R>> mapper) {
    return state -> {
      Tuple2<S, A> run = run(state);
      State<S, R> narrowK = mapper.andThen(StateOf::<S, R>toState).apply(run.get2());
      return narrowK.run(run.get1());
    };
  }

  @Override
  default <R> State<S, R> andThen(Kind<State<S, ?>, ? extends R> next) {
    return flatMap(ignore -> next);
  }

  default A eval(S state) {
    return run(state).get2();
  }

  default S runS(S state) {
    return run(state).get1();
  }

  static <S, A> State<S, A> state(Function1<S, Tuple2<S, A>> runState) {
    return runState::apply;
  }

  static <S, A> State<S, A> pure(A value) {
    return state -> Tuple2.of(state, value);
  }

  static <S> State<S, S> get() {
    return state -> Tuple2.of(state, state);
  }

  static <S> State<S, Unit> set(S value) {
    return state -> Tuple2.of(value, unit());
  }

  static <S> State<S, Unit> modify(Operator1<S> mapper) {
    return state -> Tuple2.of(mapper.apply(state), unit());
  }

  static <S, A> State<S, A> inspect(Function1<? super S, ? extends A> mapper) {
    return state -> Tuple2.of(state, mapper.apply(state));
  }

  static <S, A> State<S, Sequence<A>> traverse(Sequence<State<S, A>> states) {
    return states.foldLeft(pure(empty()),
        (State<S, Sequence<A>>sa, State<S, A> sb) -> map2(sa, sb, Sequence::append));
  }

  static <S, A, B, C> State<S, C> map2(State<S, ? extends A> sa, State<S, ? extends B> sb,
                                       Function2<? super A, ? super B, ? extends C> mapper) {
    return sa.flatMap(a -> sb.map(b -> mapper.curried().apply(a).apply(b)));
  }
}
