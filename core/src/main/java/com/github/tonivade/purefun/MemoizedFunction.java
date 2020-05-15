/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import static java.util.Collections.synchronizedMap;
import static com.github.tonivade.purefun.Precondition.checkNonNull;

import java.util.HashMap;
import java.util.Map;

final class MemoizedFunction<T, R> implements Function1<T, R> {

  private final Map<T, R> cache = synchronizedMap(new HashMap<>());
  private final Function1<T, R> function;

  MemoizedFunction(Function1<T, R> function) {
    this.function = checkNonNull(function);
  }

  @Override
  public R run(T value) {
    return cache.computeIfAbsent(value, function::apply);
  }

  @Override
  public Function1<T, R> memoized() {
    return this;
  }
}