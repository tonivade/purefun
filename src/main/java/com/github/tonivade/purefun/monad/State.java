/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.monad;

import static com.github.tonivade.purefun.Nothing.nothing;
import static com.github.tonivade.purefun.data.ImmutableList.empty;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Function2;
import com.github.tonivade.purefun.Functor;
import com.github.tonivade.purefun.Nothing;
import com.github.tonivade.purefun.Operator1;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.data.Sequence;

@FunctionalInterface
public interface State<S, A> extends Functor<A> {

  Tuple2<S, A> run(S state);

  @Override
  default <R> State<S, R> map(Function1<A, R> mapper) {
    return flatMap(value -> unit(mapper.apply(value)));
  }

  default <R> State<S, R> flatMap(Function1<A, State<S, R>> mapper) {
    return state -> {
      Tuple2<S, A> run = run(state);
      return mapper.apply(run.get2()).run(run.get1());
    };
  }

  default A eval(S state) {
    return run(state).get2();
  }

  static <S, A> State<S, A> state(Function1<S, Tuple2<S, A>> runState) {
    return runState::apply;
  }

  static <S, A> State<S, A> unit(A value) {
    return state -> Tuple2.of(state, value);
  }

  static <S> State<S, S> get() {
    return state -> Tuple2.of(state, state);
  }

  static <S> State<S, Nothing> set(S value) {
    return state -> Tuple2.of(value, nothing());
  }

  static <S> State<S, Nothing> modify(Operator1<S> mapper) {
    return state -> Tuple2.of(mapper.apply(state), nothing());
  }

  static <S, A> State<S, A> gets(Function1<S, A> mapper) {
    return state -> Tuple2.of(state, mapper.apply(state));
  }

  static <S, A> State<S, Sequence<A>> compose(Sequence<State<S, A>> states) {
    return states.foldLeft(unit(empty()), (sa, sb) -> map2(sa, sb, (acc, a) -> acc.append(a)));
  }

  static <S, A, B, C> State<S, C> map2(State<S, A> sa, State<S, B> sb, Function2<A, B, C> mapper) {
    return sa.flatMap(a -> sb.map(b -> mapper.curried().apply(a).apply(b)));
  }
}
