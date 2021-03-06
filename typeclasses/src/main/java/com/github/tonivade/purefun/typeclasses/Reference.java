/*
 * Copyright (c) 2018-2021, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.Precondition.checkNonNull;

import java.util.concurrent.atomic.AtomicReference;

import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Witness;
import com.github.tonivade.purefun.Operator1;
import com.github.tonivade.purefun.Unit;

public interface Reference<F extends Witness, A> {

  Kind<F, A> get();

  Kind<F, Unit> set(A newValue);

  Kind<F, A> getAndSet(A newValue);

  Kind<F, A> updateAndGet(Operator1<A> update);

  Kind<F, A> getAndUpdate(Operator1<A> update);

  static <F extends Witness, A> Reference<F, A> of(MonadDefer<F> monadF, A value) {
    return new MonadDeferReference<>(monadF, new AtomicReference<>(value));
  }
}

final class MonadDeferReference<F extends Witness, A> implements Reference<F, A> {

  private final MonadDefer<F> monadF;
  private final AtomicReference<A> value;

  MonadDeferReference(MonadDefer<F> monadF, AtomicReference<A> value) {
    this.monadF = checkNonNull(monadF);
    this.value = checkNonNull(value);
  }

  @Override
  public Kind<F, A> get() {
    return monadF.later(value::get);
  }

  @Override
  public Kind<F, Unit> set(A newValue) {
    return monadF.exec(() -> value.set(newValue));
  }

  @Override
  public Kind<F, A> getAndSet(A newValue) {
    return monadF.later(() -> value.getAndSet(newValue));
  }

  @Override
  public Kind<F, A> updateAndGet(Operator1<A> update) {
    return monadF.later(() -> value.updateAndGet(update::apply));
  }

  @Override
  public Kind<F, A> getAndUpdate(Operator1<A> update) {
    return monadF.later(() -> value.getAndUpdate(update::apply));
  }

  @Override
  public String toString() {
    return String.format("Reference(%s)", value.get());
  }
}
