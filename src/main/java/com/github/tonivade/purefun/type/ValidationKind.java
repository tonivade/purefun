/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.type;

import com.github.tonivade.purefun.Higher;
import com.github.tonivade.purefun.Higher2;
import com.github.tonivade.purefun.Witness;

public interface ValidationKind<E, T> extends Higher2<ValidationKind.µ, E, T> {

  final class µ implements Witness {}

  static <E, T> Validation<E, T> narrowK(Higher2<ValidationKind.µ, E, T> hkt) {
    return (Validation<E, T>) hkt;
  }

  static <E, T> Validation<E, T> narrowK(Higher<Higher<ValidationKind.µ, E>, T> hkt) {
    return (Validation<E, T>) hkt;
  }
}
