/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.type;

import com.github.tonivade.purefun.Higher;
import com.github.tonivade.purefun.Witness;

public interface TryKind<T> extends Higher<TryKind.µ, T> {

  final class µ implements Witness {}

  static <T> Try<T> narrowK(Higher<TryKind.µ, T> hkt) {
    return (Try<T>) hkt;
  }
}
