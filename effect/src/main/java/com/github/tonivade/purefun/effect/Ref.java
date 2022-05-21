/*
 * Copyright (c) 2018-2022, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.effect;

import static com.github.tonivade.purefun.Precondition.checkNonNull;
import static com.github.tonivade.purefun.Unit.unit;

import java.util.concurrent.atomic.AtomicReference;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Operator1;
import com.github.tonivade.purefun.Tuple2;
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
  
  public <B> UIO<B> modify(Function1<A, Tuple2<B, A>> change) {
    return UIO.task(() -> {
      var loop = true;
      B result = null;
      while (loop) {
        A current = value.get();
        var tuple = change.apply(current);
        result = tuple.get1();
        loop = !value.compareAndSet(current, tuple.get2());
      }
      return result;
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
    return new Ref<>(new AtomicReference<>(value));
  }

  @Override
  public String toString() {
    return String.format("Ref(%s)", value.get());
  }
}
