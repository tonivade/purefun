/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

public class State<S, A> {
  
  private final Handler1<S, Tupple2<A, S>> runState;
  
  private State(Handler1<S, Tupple2<A, S>> runState) {
    this.runState = runState;
  }
  
  public static <S, A> State<S, A> unit(A value) {
    return new State<>(state -> Tupple2.of(value, state));
  }
  
  public <R> State<S, R> flatMap(Handler1<A, State<S, R>> map) {
    return new State<>(state ->
    {
      Tupple2<A, S> tempState = run(state);
      return map.handle(tempState.get1()).run(tempState.get2()); 
    });
  }
  
  public <R> State<S, R> map(Handler1<A, R> map) {
    return flatMap(value -> unit(map.handle(value)));
  }
 
  public Tupple2<A, S> run(S state) {
    return runState.handle(state);
  }

  public A eval(S state) {
    return runState.handle(state).get1();
  }
}
