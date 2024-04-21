/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.core.Precondition.checkNonNull;

import java.util.concurrent.atomic.AtomicReference;

import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Witness;
import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Operator1;
import com.github.tonivade.purefun.core.Tuple2;
import com.github.tonivade.purefun.core.Unit;
import com.github.tonivade.purefun.type.Option;

public interface Reference<F extends Witness, A> {

  Kind<F, A> get();

  Kind<F, Unit> set(A newValue);

  <B> Kind<F, B> modify(Function1<A, Tuple2<B, A>> change);

  Kind<F, A> getAndSet(A newValue);

  Kind<F, A> updateAndGet(Operator1<A> update);

  Kind<F, A> getAndUpdate(Operator1<A> update);

  static <F extends Witness, A> Reference<F, A> of(MonadDefer<F> monadF, A value) {
    return new MonadDeferReference<>(monadF, value);
  }
}

final class MonadDeferReference<F extends Witness, A> implements Reference<F, A> {

  private final MonadDefer<F> monadF;
  private final AtomicReference<A> value;

  MonadDeferReference(MonadDefer<F> monadF, A value) {
    this.monadF = checkNonNull(monadF);
    this.value = new AtomicReference<>(checkNonNull(value));
  }

  @Override
  public Kind<F, A> get() {
    return monadF.later(this::safeGet);
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
        A current = safeGet();
        var tuple = change.apply(current);
        result = tuple.get1();
        loop = !value.compareAndSet(current, tuple.get2());
      }
      return Option.of(result).getOrElseThrow();
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

  @SuppressWarnings("NullAway")
  private A safeGet() {
    return value.get();
  }
}
