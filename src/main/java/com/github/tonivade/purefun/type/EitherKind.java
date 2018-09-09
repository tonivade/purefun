/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.type;

import com.github.tonivade.purefun.Higher;
import com.github.tonivade.purefun.Higher2;
import com.github.tonivade.purefun.Witness;

public interface EitherKind<L, R> extends Higher2<EitherKind.µ, L, R> {

  final class µ implements Witness {}

  static <L, R> Either<L, R> narrowK(Higher2<EitherKind.µ, L, R> hkt) {
    return (Either<L, R>) hkt;
  }

  static <L, R> Either<L, R> narrowK(Higher<Higher<EitherKind.µ, L>, R> hkt) {
    return (Either<L, R>) hkt;
  }
}
