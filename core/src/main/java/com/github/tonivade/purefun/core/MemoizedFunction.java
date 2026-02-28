/*
 * Copyright (c) 2018-2026, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.core;

import static com.github.tonivade.purefun.core.Precondition.checkNonNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class MemoizedFunction<T, R> implements Function1<T, R> {

  private final Map<T, R> cache = new ConcurrentHashMap<>();
  private final Function1<? super T, ? extends R> function;

  MemoizedFunction(Function1<? super T, ? extends R> function) {
    this.function = checkNonNull(function);
  }

  @Override
  public R run(T value) {
    return cache.computeIfAbsent(value, function);
  }

  @Override
  public Function1<T, R> memoized() {
    return this;
  }
}