/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

public interface Handler4<A, B, C, D, R> {

  R handle(A t1, B t2, C t3, D t4);
  
  default Handler1<A, Handler1<B, Handler1<C, Handler1<D, R>>>> curried() {
    return t1 -> t2 -> t3 -> t4 -> handle(t1, t2, t3, t4);
  }
  
  default <U> Handler4<A, B, C, D, U> andThen(Handler1<R, U> after) {
    return (t1, t2, t3, t4) -> after.handle(handle(t1, t2, t3, t4));
  }
  
  default <U> Handler1<U, R> compose(Handler1<U, A> beforeT1, Handler1<U, B> beforeT2, 
      Handler1<U, C> beforeT3, Handler1<U, D> beforeT4) {
    return value -> handle(beforeT1.handle(value), beforeT2.handle(value), beforeT3.handle(value), beforeT4.handle(value));
  }
}

