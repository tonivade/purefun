/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.monad;

import static com.github.tonivade.purefun.Nothing.nothing;
import static com.github.tonivade.purefun.data.ImmutableList.empty;

import com.github.tonivade.purefun.FlatMap2;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Function2;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Higher2;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Nothing;
import com.github.tonivade.purefun.Operator1;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.typeclasses.Monad;

@FunctionalInterface
public interface State<S, A> extends FlatMap2<State.µ, S, A> {

  final class µ implements Kind {}

  Tuple2<S, A> run(S state);

  @Override
  default <R> State<S, R> map(Function1<A, R> mapper) {
    return flatMap(value -> pure(mapper.apply(value)));
  }

  @Override
  default <R> State<S, R> flatMap(Function1<A, ? extends Higher2<State.µ, S, R>> mapper) {
    return state -> {
      Tuple2<S, A> run = run(state);
      return mapper.andThen(State::narrowK).apply(run.get2()).run(run.get1());
    };
  }

  default A eval(S state) {
    return run(state).get2();
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

  static <S> State<S, Nothing> set(S value) {
    return state -> Tuple2.of(value, nothing());
  }

  static <S> State<S, Nothing> modify(Operator1<S> mapper) {
    return state -> Tuple2.of(mapper.apply(state), nothing());
  }

  static <S, A> State<S, A> inspect(Function1<S, A> mapper) {
    return state -> Tuple2.of(state, mapper.apply(state));
  }

  static <S, A> State<S, Sequence<A>> compose(Sequence<State<S, A>> states) {
    return states.foldLeft(pure(empty()), (sa, sb) -> map2(sa, sb, (acc, a) -> acc.append(a)));
  }

  static <S, A, B, C> State<S, C> map2(State<S, A> sa, State<S, B> sb, Function2<A, B, C> mapper) {
    return sa.flatMap(a -> sb.map(b -> mapper.curried().apply(a).apply(b)));
  }

  static <S, A> State<S, A> narrowK(Higher2<State.µ, S, A> hkt) {
    return (State<S, A>) hkt;
  }

  static <S, A> State<S, A> narrowK(Higher1<Higher1<State.µ, S>, A> hkt) {
    return (State<S, A>) hkt;
  }

  static <V> Monad<Higher1<State.µ, V>> monad() {
    return new Monad<Higher1<State.µ, V>>() {

      @Override
      public <T> State<V, T> pure(T value) {
        return State.pure(value);
      }

      @Override
      public <T, R> State<V, R> flatMap(Higher1<Higher1<State.µ, V>, T> value,
                                        Function1<T, ? extends Higher1<Higher1<State.µ, V>, R>> map) {
        return narrowK(value).flatMap(map.andThen(State::narrowK));
      }
    };
  }
}
