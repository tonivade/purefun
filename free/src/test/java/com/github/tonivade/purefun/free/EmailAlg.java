package com.github.tonivade.purefun.free;

import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Unit;

import static java.util.Objects.requireNonNull;

@HigherKind
public interface EmailAlg<T> {
  final class SendEmail implements EmailAlg<Unit> {
    final String to;
    final String content;
    SendEmail(String to, String content) {
      this.to = requireNonNull(to);
      this.content = requireNonNull(content);
    }
  }
}
