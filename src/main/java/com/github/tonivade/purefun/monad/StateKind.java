/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.monad;

import static java.util.Objects.requireNonNull;

import com.github.tonivade.purefun.Higher2;
import com.github.tonivade.purefun.Tuple2;

final class StateKind<S, A> implements State<S, A>, Higher2<StateKind.µ, S, A>{

  public static final class µ {}

  private final State<S, A> delegate;

  public StateKind(State<S, A> delegate) {
    this.delegate = requireNonNull(delegate);
  }

  @Override
  public Tuple2<S, A> run(S state) {
    return delegate.run(state);
  }

  public static <S, A> State<S, A> narrowK(Higher2<StateKind.µ, S, A> hkt) {
    return (State<S, A>) hkt;
  }
}
