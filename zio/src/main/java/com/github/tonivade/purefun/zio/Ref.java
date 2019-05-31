/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.zio;

import static com.github.tonivade.purefun.Unit.unit;
import static java.util.Objects.requireNonNull;

import java.util.concurrent.atomic.AtomicReference;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Unit;

public final class Ref<A> {

  private final AtomicReference<A> value;

  private Ref(AtomicReference<A> value) {
    this.value = requireNonNull(value);
  }

  public <R, E> ZIO<R, E, A> get() {
    return ZIO.pure(value::get);
  }

  public <R, E> ZIO<R, E, Unit> set(A newValue) {
    return ZIO.pure(() -> { value.set(newValue); return unit(); });
  }

  public <R, E> ZIO<R, E, Unit> lazySet(A newValue) {
    return ZIO.pure(() -> { value.lazySet(newValue); return unit(); });
  }

  public <R, E> ZIO<R, E, A> getAndSet(A newValue) {
    return ZIO.pure(() -> value.getAndSet(newValue));
  }

  public <R, E> ZIO<R, E, A> updateAndGet(Function1<A, A> update) {
    return ZIO.pure(() -> value.updateAndGet(update::apply));
  }

  public <R, E> ZIO<R, E, A> getAndUpdate(Function1<A, A> update) {
    return ZIO.pure(() -> value.getAndUpdate(update::apply));
  }

  public static <R, E, A> Ref<A> of(A value) {
    return new Ref<>(new AtomicReference<>(value));
  }

  @Override
  public String toString() {
    return "Ref(" + value.get() + ")";
  }
}
