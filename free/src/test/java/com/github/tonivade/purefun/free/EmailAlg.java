package com.github.tonivade.purefun.free;

import static com.github.tonivade.purefun.core.Precondition.checkNonNull;

import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.core.Unit;

@HigherKind
public sealed interface EmailAlg<T> extends EmailAlgOf<T> {

  record SendEmail(String to, String content) implements EmailAlg<Unit> {
    public SendEmail {
      checkNonNull(to);
      checkNonNull(content);
    }
  }
}
