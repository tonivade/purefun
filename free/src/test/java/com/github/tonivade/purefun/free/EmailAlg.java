package com.github.tonivade.purefun.free;

import static com.github.tonivade.purefun.Precondition.checkNonNull;

import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.annotation.HigherKind;

@HigherKind
public sealed interface EmailAlg<T> extends EmailAlgOf<T> {

  record SendEmail(String to, String content) implements EmailAlg<Unit> {
    public SendEmail {
      checkNonNull(to);
      checkNonNull(content);
    }
  }
}
