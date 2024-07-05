package com.github.tonivade.purefun.free;

import static com.github.tonivade.purefun.core.Precondition.checkNonNull;

import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.core.Unit;

public sealed interface ConsoleAlg<T> extends Kind<ConsoleAlg<?>, T> {

  record WriteLine(String line) implements ConsoleAlg<Unit> {
    public WriteLine {
      checkNonNull(line);
    }
  }

  record ReadLine() implements ConsoleAlg<String> { }
}
