/*
 * Copyright (c) 2018-2021, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.effect;

import static com.github.tonivade.purefun.Unit.unit;
import static com.github.tonivade.purefun.Precondition.checkNonNull;

import java.util.concurrent.atomic.AtomicReference;

import com.github.tonivade.purefun.Operator1;
import com.github.tonivade.purefun.Unit;

public final class Ref<A> {

  private final AtomicReference<A> value;

  private Ref(AtomicReference<A> value) {
    this.value = checkNonNull(value);
  }

  public UIO<A> get() {
    return UIO.task(value::get);
  }

  public UIO<Unit> set(A newValue) {
    return UIO.task(() -> { value.set(newValue); return unit(); });
  }

  public UIO<Unit> lazySet(A newValue) {
    return UIO.task(() -> { value.lazySet(newValue); return unit(); });
  }

  public UIO<A> getAndSet(A newValue) {
    return UIO.task(() -> value.getAndSet(newValue));
  }

  public UIO<A> updateAndGet(Operator1<A> update) {
    return UIO.task(() -> value.updateAndGet(update::apply));
  }

  public UIO<A> getAndUpdate(Operator1<A> update) {
    return UIO.task(() -> value.getAndUpdate(update::apply));
  }

  public static <A> Ref<A> of(A value) {
    return new Ref<>(new AtomicReference<>(value));
  }

  @Override
  public String toString() {
    return String.format("Ref(%s)", value.get());
  }
}
