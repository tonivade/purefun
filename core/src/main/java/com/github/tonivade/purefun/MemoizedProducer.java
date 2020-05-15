/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import static com.github.tonivade.purefun.Unit.unit;
import static java.util.Collections.synchronizedMap;
import static com.github.tonivade.purefun.Precondition.checkNonNull;

import java.util.HashMap;
import java.util.Map;

final class MemoizedProducer<T> implements Producer<T> {

  private final Map<Unit, T> cache = synchronizedMap(new HashMap<>(1));
  private final Function1<Unit, T> function;

  MemoizedProducer(Producer<T> producer) {
    this.function = checkNonNull(producer).asFunction();
  }

  @Override
  public T run() {
    return cache.computeIfAbsent(unit(), function::apply);
  }

  @Override
  public Producer<T> memoized() {
    return this;
  }
}
