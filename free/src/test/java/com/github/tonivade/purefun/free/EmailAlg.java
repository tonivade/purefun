package com.github.tonivade.purefun.free;

import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Unit;

import static java.util.Objects.requireNonNull;

@HigherKind
public interface EmailAlg<T> {
  final class SendEmail implements EmailAlg<Unit> {
    private final String to;
    private final String content;

    public SendEmail(String to, String content) {
      this.to = requireNonNull(to);
      this.content = requireNonNull(content);
    }

    public String getTo() { return to; }

    public String getContent() { return content; }
  }
}
