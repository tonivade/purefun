/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import java.util.concurrent.Executor;

import com.github.tonivade.purefun.Kind;

import com.github.tonivade.purefun.concurrent.Future;
import com.github.tonivade.purefun.data.Sequence;

public interface Runtime<F> {

  <T> T run(Kind<F, T> value);

  <T> Sequence<T> run(Sequence<Kind<F, T>> values);
  
  <T> Future<T> parRun(Kind<F, T> value, Executor executor);
  
  default <T> Future<T> parRun(Kind<F, T> value) {
    return parRun(value, Future.DEFAULT_EXECUTOR);
  }
  
  <T> Future<Sequence<T>> parRun(Sequence<Kind<F, T>> values, Executor executor);

  default <T> Future<Sequence<T>> parRun(Sequence<Kind<F, T>> values) {
    return parRun(values, Future.DEFAULT_EXECUTOR);
  }
}