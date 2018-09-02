/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.data;

import com.github.tonivade.purefun.Higher;
import com.github.tonivade.purefun.Witness;

public interface SequenceKind<T> extends Higher<SequenceKind.µ, T> {

  final class µ implements Witness {}

  static <T> Sequence<T> narrowK(Higher<SequenceKind.µ, T> hkt) {
    return (Sequence<T>) hkt;
  }
}
