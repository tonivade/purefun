/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.monad;

import com.github.tonivade.purefun.Higher2;
import com.github.tonivade.purefun.Witness;

public interface WriterKind<L, T> extends Higher2<WriterKind.µ, L, T> {

  final class µ implements Witness {}

  public static <L, T> Writer<L, T> narrowK(Higher2<WriterKind.µ, L, T> hkt) {
    return (Writer<L, T>) hkt;
  }
}
