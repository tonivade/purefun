/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import static com.github.tonivade.purefun.Nothing.nothing;
import static java.util.Objects.requireNonNull;

public final class State<S, A> {
  
  private final Function1<S, Tuple2<S, A>> runState;
  
  private State(Function1<S, Tuple2<S, A>> runState) {
    this.runState = requireNonNull(runState);
  }
  
  public static <S, A> State<S, A> state(Function1<S, Tuple2<S, A>> runState) {
    return new State<>(runState);
  }
  
  public static <S, A> State<S, A> unit(A value) {
    return new State<>(state -> Tuple2.of(state, value));
  }
  
  public static <S> State<S, S> get() {
    return new State<>(state -> Tuple2.of(state, state));
  }
  
  public static <S> State<S, Nothing> set(S value) {
    return new State<>(state -> Tuple2.of(value, nothing()));
  }
  
  public static <S> State<S, Nothing> modify(Operator1<S> mapper) {
    return new State<>(state -> Tuple2.of(mapper.apply(state), nothing()));
  }
  
  public static <S, A> State<S, A> gets(Function1<S, A> mapper) {
    return new State<>(state -> Tuple2.of(state, mapper.apply(state)));
  }
  
  public static <S, A> State<S, ImmutableList<A>> compose(Sequence<State<S, A>> states) {
    return states.foldLeft(unit(ImmutableList.empty()), (sa, sb) -> map2(sa, sb, (acc, a) -> acc.append(a)));
  }
  
  public static <S, A, B, C> State<S, C> map2(State<S, A> sa, State<S, B> sb, 
                                              Function2<A, B, C> mapper) {
    return sa.flatMap(a -> sb.map(b -> mapper.curried().apply(a).apply(b)));
  }
  
  public <R> State<S, R> flatMap(Function1<A, State<S, R>> mapper) {
    return new State<>(state -> apply(run(state), mapper));
  }
  
  public <R> State<S, R> map(Function1<A, R> mapper) {
    return flatMap(value -> unit(mapper.apply(value)));
  }
 
  public Tuple2<S, A> run(S state) {
    return runState.apply(state);
  }

  public A eval(S state) {
    return run(state).get2();
  }

  private static <S, A, R> Tuple2<S, R> apply(Tuple2<S, A> state, Function1<A, State<S, R>> mapper) {
    return mapper.apply(state.get2()).run(state.get1());
  }
}
