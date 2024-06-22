package com.github.tonivade.purefun.free;

import static com.github.tonivade.purefun.core.Precondition.checkNonNull;

import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.core.Unit;

public sealed interface EmailAlg<T> extends Kind<EmailAlg<?>, T> {

  record SendEmail(String to, String content) implements EmailAlg<Unit> {
    public SendEmail {
      checkNonNull(to);
      checkNonNull(content);
    }
  }
}
