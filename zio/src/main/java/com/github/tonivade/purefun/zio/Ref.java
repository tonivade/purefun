/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.zio;

import static com.github.tonivade.purefun.Unit.unit;
import static java.util.Objects.requireNonNull;

import java.util.concurrent.atomic.AtomicReference;

import com.github.tonivade.purefun.Operator1;
import com.github.tonivade.purefun.Unit;

public final class Ref<R, E, A> {

  private final AtomicReference<A> value;

  private Ref(AtomicReference<A> value) {
    this.value = requireNonNull(value);
  }

  public ZIO<R, E, A> get() {
    return ZIO.task(value::get);
  }

  public ZIO<R, E, Unit> set(A newValue) {
    return ZIO.task(() -> { value.set(newValue); return unit(); });
  }

  public ZIO<R, E, Unit> lazySet(A newValue) {
    return ZIO.task(() -> { value.lazySet(newValue); return unit(); });
  }

  public ZIO<R, E, A> getAndSet(A newValue) {
    return ZIO.task(() -> value.getAndSet(newValue));
  }

  public ZIO<R, E, A> updateAndGet(Operator1<A> update) {
    return ZIO.task(() -> value.updateAndGet(update::apply));
  }

  public ZIO<R, E, A> getAndUpdate(Operator1<A> update) {
    return ZIO.task(() -> value.getAndUpdate(update::apply));
  }

  public static <R, E, A> Ref<R, E, A> of(A value) {
    return new Ref<>(new AtomicReference<>(value));
  }

  @Override
  public String toString() {
    return "Ref(" + value.get() + ")";
  }
}
