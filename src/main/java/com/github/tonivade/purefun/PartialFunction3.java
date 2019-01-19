/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import com.github.tonivade.purefun.type.Option;

public interface PartialFunction3<A, B, C, R> {

  R apply(A a, B b, C c);

  boolean isDefinedAt(A a, B b, C c);

  default Function3<A, B, C, Option<R>> lift() {
    return (a, b, c) -> isDefinedAt(a, b, c) ? Option.some(apply(a, b, c)) : Option.none();
  }
}
