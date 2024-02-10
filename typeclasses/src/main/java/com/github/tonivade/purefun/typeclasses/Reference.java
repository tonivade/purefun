/*
 * Copyright (c) 2018-2023, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.Precondition.checkNonNull;

import java.util.concurrent.atomic.AtomicReference;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Operator1;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.annotation.Kind;
import com.github.tonivade.purefun.annotation.Witness;

public interface Reference<F extends Witness, A> {

  Kind<F, A> get();

  Kind<F, Unit> set(A newValue);

  <B> Kind<F, B> modify(Function1<A, Tuple2<B, A>> change);

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
  public <B> Kind<F, B> modify(Function1<A, Tuple2<B, A>> change) {
    return monadF.later(() -> {
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
