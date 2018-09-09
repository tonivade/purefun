package com.github.tonivade.purefun.type;

import com.github.tonivade.purefun.Higher;
import com.github.tonivade.purefun.Witness;

public interface IdKind<T> extends Higher<IdKind.µ, T> {

public final class µ implements Witness {}

  static <T> Id<T> narrowK(Higher<IdKind.µ, T> hkt) {
    return (Id<T>) hkt;
  }
}
