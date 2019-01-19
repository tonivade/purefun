/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import static com.github.tonivade.purefun.Nothing.nothing;
import static java.util.Collections.synchronizedMap;
import static java.util.Objects.requireNonNull;

import java.util.HashMap;
import java.util.Map;

final class MemoizedProducer<T> implements Producer<T> {

  private final Map<Nothing, T> cache = synchronizedMap(new HashMap<>());
  private final Function1<Nothing, T> function;

  MemoizedProducer(Producer<T> producer) {
    this.function = requireNonNull(producer).asFunction();
  }

  @Override
  public T get() {
    return cache.computeIfAbsent(nothing(), function::apply);
  }

  @Override
  public Producer<T> memoized() {
    return this;
  }
}
