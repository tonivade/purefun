/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.Unit.unit;
import static java.util.Objects.requireNonNull;

import java.util.concurrent.atomic.AtomicReference;

import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Operator1;
import com.github.tonivade.purefun.TypeClass;
import com.github.tonivade.purefun.Unit;

@TypeClass
public interface Reference<F extends Kind, A> {

  Higher1<F, A> get();

  Higher1<F, Unit> set(A newValue);

  Higher1<F, A> getAndSet(A newValue);

  Higher1<F, A> updateAndGet(Operator1<A> update);

  Higher1<F, A> getAndUpdate(Operator1<A> update);

  static <F extends Kind, A> Reference<F, A> of(MonadDefer<F> monadF, A value) {
    return new MonadDeferReference<>(monadF, new AtomicReference<>(value));
  }
}

final class MonadDeferReference<F extends Kind, A> implements Reference<F, A> {

  private final MonadDefer<F> monadF;
  private final AtomicReference<A> value;

  MonadDeferReference(MonadDefer<F> monadF, AtomicReference<A> value) {
    this.monadF = requireNonNull(monadF);
    this.value = requireNonNull(value);
  }

  @Override
  public Higher1<F, A> get() {
    return monadF.later(value::get);
  }

  @Override
  public Higher1<F, Unit> set(A newValue) {
    return monadF.later(() -> { value.set(newValue); return unit(); });
  }

  @Override
  public Higher1<F, A> getAndSet(A newValue) {
    return monadF.later(() -> value.getAndSet(newValue));
  }

  @Override
  public Higher1<F, A> updateAndGet(Operator1<A> update) {
    return monadF.later(() -> value.updateAndGet(update::apply));
  }

  @Override
  public Higher1<F, A> getAndUpdate(Operator1<A> update) {
    return monadF.later(() -> value.getAndUpdate(update::apply));
  }

  @Override
  public String toString() {
    return String.format("Reference(%s)", value.get());
  }
}
