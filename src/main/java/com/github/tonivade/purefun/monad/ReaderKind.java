/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.monad;

import com.github.tonivade.purefun.Higher2;

public interface ReaderKind<R, A> extends Higher2<ReaderKind.µ, R, A>{

  final class µ {}

  static <R, A> Reader<R, A> narrowK(Higher2<ReaderKind.µ, R, A> hkt) {
    return (Reader<R, A>) hkt;
  }
}
