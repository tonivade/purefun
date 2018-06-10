/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

public interface Handler5<T1, T2, T3, T4, T5, R> {

  R handle(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5);
  
  default Handler1<T1, Handler1<T2, Handler1<T3, Handler1<T4, Handler1<T5, R>>>>> curried() {
    return t1 -> t2 -> t3 -> t4 -> t5 -> handle(t1, t2, t3, t4, t5);
  }
  
  default <U> Handler5<T1, T2, T3, T4, T5, U> andThen(Handler1<R, U> after) {
    return (t1, t2, t3, t4, t5) -> after.handle(handle(t1, t2, t3, t4, t5));
  }
  
  default <U> Handler1<U, R> compose(Handler1<U, T1> beforeT1, Handler1<U, T2> beforeT2, 
      Handler1<U, T3> beforeT3, Handler1<U, T4> beforeT4, Handler1<U, T5> beforeT5) {
    return value -> handle(beforeT1.handle(value), beforeT2.handle(value), 
        beforeT3.handle(value), beforeT4.handle(value), beforeT5.handle(value));
  }
}

