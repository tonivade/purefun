package com.github.tonivade.purefun.free;

import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Unit;

import static com.github.tonivade.purefun.Precondition.checkNonNull;

import com.github.tonivade.purefun.Higher1;

@HigherKind
public interface EmailAlg<T> extends Higher1<EmailAlg_, T> {
  final class SendEmail implements EmailAlg<Unit> {
    private final String to;
    private final String content;

    public SendEmail(String to, String content) {
      this.to = checkNonNull(to);
      this.content = checkNonNull(content);
    }

    public String getTo() { return to; }

    public String getContent() { return content; }
  }
}
