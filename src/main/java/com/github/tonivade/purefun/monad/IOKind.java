/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.monad;

import com.github.tonivade.purefun.Higher;
import com.github.tonivade.purefun.Witness;

public interface IOKind<T> extends Higher<IOKind.µ, T>{

  final class µ implements Witness {}

  static <T> IO<T> narrowK(Higher<IOKind.µ, T> hkt) {
    return (IO<T>) hkt;
  }
}
