/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.type;

import com.github.tonivade.purefun.Higher;

public interface OptionKind<T> extends Higher<OptionKind.µ, T> {

  final class µ {}

  static <T> Option<T> narrowK(Higher<OptionKind.µ, T> hkt) {
    return (Option<T>) hkt;
  }
}
