/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

public interface Handler5<A, B, C, D, E, R> {

  R handle(A t1, B t2, C t3, D t4, E t5);
  
  default Handler1<A, Handler1<B, Handler1<C, Handler1<D, Handler1<E, R>>>>> curried() {
    return t1 -> t2 -> t3 -> t4 -> t5 -> handle(t1, t2, t3, t4, t5);
  }
  
  default <U> Handler5<A, B, C, D, E, U> andThen(Handler1<R, U> after) {
    return (t1, t2, t3, t4, t5) -> after.handle(handle(t1, t2, t3, t4, t5));
  }
  
  default <U> Handler1<U, R> compose(Handler1<U, A> beforeT1, Handler1<U, B> beforeT2, 
      Handler1<U, C> beforeT3, Handler1<U, D> beforeT4, Handler1<U, E> beforeT5) {
    return value -> handle(beforeT1.handle(value), beforeT2.handle(value), 
        beforeT3.handle(value), beforeT4.handle(value), beforeT5.handle(value));
  }
}

