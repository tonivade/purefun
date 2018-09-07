package com.github.tonivade.purefun.monad;

import com.github.tonivade.purefun.Higher2;
import com.github.tonivade.purefun.Witness;

public interface FreeKind<F, T> extends Higher2<FreeKind.µ, F, T> {

  final class µ implements Witness {}

  static <F extends Witness, T> Free<F, T> narrowK(Higher2<FreeKind.µ, F, T> hkt) {
    return (Free<F, T>) hkt;
  }
}