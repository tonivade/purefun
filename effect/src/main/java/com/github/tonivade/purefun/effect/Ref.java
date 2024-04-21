/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.effect;

import static com.github.tonivade.purefun.core.Precondition.checkNonNull;
import static com.github.tonivade.purefun.core.Unit.unit;

import java.util.concurrent.atomic.AtomicReference;

import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Operator1;
import com.github.tonivade.purefun.core.Tuple2;
import com.github.tonivade.purefun.core.Unit;
import com.github.tonivade.purefun.type.Option;

public final class Ref<A> {

  private final AtomicReference<A> value;

  private Ref(A value) {
    this.value = new AtomicReference<>(checkNonNull(value));
  }

  public UIO<A> get() {
    return UIO.task(this::safeGet);
  }

  public UIO<Unit> set(A newValue) {
    return UIO.exec(() -> value.set(newValue));
  }

  public <B> UIO<B> modify(Function1<A, Tuple2<B, A>> change) {
    return UIO.task(() -> {
      var loop = true;
      B result = null;
      while (loop) {
        A current = safeGet();
        var tuple = change.apply(current);
        result = tuple.get1();
        loop = !value.compareAndSet(current, tuple.get2());
      }
      return Option.of(result).getOrElseThrow();
    });
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

  public static <A> UIO<Ref<A>> make(A value) {
    return UIO.pure(of(value));
  }

  public static <A> Ref<A> of(A value) {
    return new Ref<>(value);
  }

  @Override
  public String toString() {
    return String.format("Ref(%s)", value.get());
  }

  @SuppressWarnings("NullAway")
  private A safeGet() {
    return value.get();
  }
}
