/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

public interface Handler4<T1, T2, T3, T4, R> {

  R handle(T1 t1, T2 t2, T3 t3, T4 t4);
  
  default Handler1<T1, Handler1<T2, Handler1<T3, Handler1<T4, R>>>> curried() {
    return t1 -> t2 -> t3 -> t4 -> handle(t1, t2, t3, t4);
  }
  
  default <U> Handler4<T1, T2, T3, T4, U> andThen(Handler1<R, U> after) {
    return (t1, t2, t3, t4) -> after.handle(handle(t1, t2, t3, t4));
  }
  
  default <U> Handler1<U, R> compose(Handler1<U, T1> beforeT1, Handler1<U, T2> beforeT2, 
      Handler1<U, T3> beforeT3, Handler1<U, T4> beforeT4) {
    return value -> handle(beforeT1.handle(value), beforeT2.handle(value), beforeT3.handle(value), beforeT4.handle(value));
  }
}

