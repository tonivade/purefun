/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.core.Precondition.checkNonNull;

import com.github.tonivade.purefun.Kind;

import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Operator1;
import com.github.tonivade.purefun.core.Tuple2;
import com.github.tonivade.purefun.core.Unit;

public interface MonadState<F extends Kind<F, ?>, S> extends Monad<F> {

  Kind<F, S> get();
  Kind<F, Unit> set(S state);

  default Kind<F, Unit> modify(Operator1<S> mapper) {
    return flatMap(get(), s -> set(mapper.apply(s)));
  }

  default <A> Kind<F, A> inspect(Function1<? super S, ? extends A> mapper) {
    return map(get(), mapper);
  }

  default <A> Kind<F, A> state(Function1<? super S, Tuple2<S, ? extends A>> mapper) {
    return flatMap(get(), s -> mapper.apply(s).applyTo((s1, a) -> map(set(s1), x -> a)));
  }

  static <F extends Kind<F, ?>, S> MonadState<F, S> from(MonadDefer<F> monad, S value) {
    return new ReferenceMonadState<>(Reference.of(monad, value), monad);
  }
}

final class ReferenceMonadState<F extends Kind<F, ?>, S> implements MonadState<F, S> {

  private final Reference<F, S> ref;
  private final Monad<F> monad;

  ReferenceMonadState(Reference<F, S> ref, Monad<F> monad) {
    this.ref = checkNonNull(ref);
    this.monad = checkNonNull(monad);
  }

  @Override
  public Kind<F, S> get() {
    return ref.get();
  }

  @Override
  public Kind<F, Unit> set(S state) {
    return ref.set(state);
  }

  @Override
  public <T> Kind<F, T> pure(T value) {
    return monad.pure(value);
  }

  @Override
  public <T, R> Kind<F, R> flatMap(Kind<F, ? extends T> value, Function1<? super T, ? extends Kind<F, ? extends R>> map) {
    return monad.flatMap(value, map);
  }
}
