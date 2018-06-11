/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

public interface Handler3<A, B, C, R> {
  
  R handle(A t1, B t2, C t3);
  
  default Handler1<A, Handler1<B, Handler1<C, R>>> curried() {
    return t1 -> t2 -> t3 -> handle(t1, t2, t3);
  }
  
  default Handler1<Tuple3<A, B, C>, R> tupled() {
    return tuple -> handle(tuple.get1(), tuple.get2(), tuple.get3());
  }
  
  default <U> Handler3<A, B, C, U> andThen(Handler1<R, U> after) {
    return (t1, t2, t3) -> after.handle(handle(t1, t2, t3));
  }
  
  default <U> Handler1<U, R> compose(Handler1<U, A> beforeT1, Handler1<U, B> beforeT2, Handler1<U, C> beforeT3) {
    return value -> handle(beforeT1.handle(value), beforeT2.handle(value), beforeT3.handle(value));
  }
}
