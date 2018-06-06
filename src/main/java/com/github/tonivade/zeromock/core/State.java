/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

public class State<S, A> {
  
  private final Handler1<S, Tupple2<S, A>> runState;
  
  public State(Handler1<S, Tupple2<S, A>> runState) {
    this.runState = runState;
  }
  
  public static <S, A> State<S, A> unit(A value) {
    return new State<>(state -> Tupple2.of(state, value));
  }
  
  public static <S, A> State<S, A> state(Handler1<S, A> handler) {
    return new State<>(state -> Tupple2.of(state, handler.handle(state)));
  }
  
  public <R> State<S, R> flatMap(Handler1<A, State<S, R>> map) {
    return new State<>(state -> apply(run(state), map));
  }
  
  public <R> State<S, R> map(Handler1<A, R> map) {
    return flatMap(value -> unit(map.handle(value)));
  }
 
  public Tupple2<S, A> run(S state) {
    return runState.handle(state);
  }

  public A eval(S state) {
    return run(state).get2();
  }

  private static <S, A, R> Tupple2<S, R> apply(Tupple2<S, A> state, Handler1<A, State<S, R>> map) {
    return map.handle(state.get2()).run(state.get1());
  }
}
