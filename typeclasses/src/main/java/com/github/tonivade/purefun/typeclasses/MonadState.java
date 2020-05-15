/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Operator1;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.TypeClass;
import com.github.tonivade.purefun.Unit;

import static com.github.tonivade.purefun.Precondition.checkNonNull;

@TypeClass
public interface MonadState<F extends Kind, S> extends Monad<F> {

  Higher1<F, S> get();
  Higher1<F, Unit> set(S state);

  default Higher1<F, Unit> modify(Operator1<S> mapper) {
    return flatMap(get(), s -> set(mapper.apply(s)));
  }

  default <A> Higher1<F, A> inspect(Function1<S, A> mapper) {
    return map(get(), mapper);
  }

  default <A> Higher1<F, A> state(Function1<S, Tuple2<S, A>> mapper) {
    return flatMap(get(), s -> mapper.apply(s).applyTo((s1, a) -> map(set(s1), x -> a)));
  }

  static <F extends Kind, S> MonadState<F, S> from(MonadDefer<F> monad, S value) {
    return new ReferenceMonadState<>(Reference.of(monad, value), monad);
  }
}

class ReferenceMonadState<F extends Kind, S> implements MonadState<F, S> {

  private final Reference<F, S> ref;
  private final Monad<F> monad;

  ReferenceMonadState(Reference<F, S> ref, Monad<F> monad) {
    this.ref = checkNonNull(ref);
    this.monad = checkNonNull(monad);
  }

  @Override
  public Higher1<F, S> get() {
    return ref.get();
  }

  @Override
  public Higher1<F, Unit> set(S state) {
    return ref.set(state);
  }

  @Override
  public <T> Higher1<F, T> pure(T value) {
    return monad.pure(value);
  }

  @Override
  public <T, R> Higher1<F, R> flatMap(Higher1<F, T> value, Function1<T, ? extends Higher1<F, R>> map) {
    return monad.flatMap(value, map);
  }
}
