/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

public interface Function4<A, B, C, D, R> {

  R apply(A t1, B t2, C t3, D t4);
  
  default Function1<A, Function1<B, Function1<C, Function1<D, R>>>> curried() {
    return t1 -> t2 -> t3 -> t4 -> apply(t1, t2, t3, t4);
  }
  
  default Function1<Tuple4<A, B, C, D>, R> tupled() {
    return tuple -> apply(tuple.get1(), tuple.get2(), tuple.get3(), tuple.get4());
  }
  
  default <U> Function4<A, B, C, D, U> andThen(Function1<R, U> after) {
    return (t1, t2, t3, t4) -> after.apply(apply(t1, t2, t3, t4));
  }
  
  default <U> Function1<U, R> compose(Function1<U, A> beforeT1, Function1<U, B> beforeT2, 
      Function1<U, C> beforeT3, Function1<U, D> beforeT4) {
    return value -> apply(beforeT1.apply(value), beforeT2.apply(value), beforeT3.apply(value), beforeT4.apply(value));
  }
}

