/*
 * Copyright (c) 2018-2023, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import static com.github.tonivade.purefun.Precondition.checkNonNull;
import static com.github.tonivade.purefun.Unit.unit;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class MemoizedProducer<T> implements Producer<T> {

  private final Map<Unit, T> cache = new ConcurrentHashMap<>();
  private final Function1<Unit, ? extends T> function;

  MemoizedProducer(Producer<? extends T> producer) {
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
