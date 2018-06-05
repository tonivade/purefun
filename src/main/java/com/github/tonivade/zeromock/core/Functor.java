package com.github.tonivade.zeromock.core;

public interface Functor<T> {
  <R> Functor<R> map(Handler1<T, R> map);
}
