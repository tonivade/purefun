/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import com.github.tonivade.purefun.type.Option;

public interface PartialFunction2<A, B, R> {

  R apply(A a, B b);

  boolean isDefinedAt(A a, B b);

  default Function2<A, B, Option<R>> lift() {
    return (a, b) -> isDefinedAt(a, b) ? Option.some(apply(a, b)) : Option.none();
  }
}
