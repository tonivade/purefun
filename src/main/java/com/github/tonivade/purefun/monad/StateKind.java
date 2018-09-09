/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.monad;

import com.github.tonivade.purefun.Higher;
import com.github.tonivade.purefun.Higher2;
import com.github.tonivade.purefun.Witness;

public interface StateKind<S, A> extends Higher2<StateKind.µ, S, A> {

  final class µ implements Witness {}

  static <S, A> State<S, A> narrowK(Higher2<StateKind.µ, S, A> hkt) {
    return (State<S, A>) hkt;
  }

  static <S, A> State<S, A> narrowK(Higher<Higher<StateKind.µ, S>, A> hkt) {
    return (State<S, A>) hkt;
  }
}
