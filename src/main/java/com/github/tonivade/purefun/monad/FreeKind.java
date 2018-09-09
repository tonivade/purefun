/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.monad;

import com.github.tonivade.purefun.Higher;
import com.github.tonivade.purefun.Higher2;
import com.github.tonivade.purefun.Witness;

public interface FreeKind<F, T> extends Higher2<FreeKind.µ, F, T> {

  final class µ implements Witness {}

  static <F extends Witness, T> Free<F, T> narrowK(Higher2<FreeKind.µ, F, T> hkt) {
    return (Free<F, T>) hkt;
  }

  static <F extends Witness, T> Free<F, T> narrowK(Higher<Higher<FreeKind.µ, F>, T> hkt) {
    return (Free<F, T>) hkt;
  }
}