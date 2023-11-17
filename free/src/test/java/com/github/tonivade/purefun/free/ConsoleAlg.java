package com.github.tonivade.purefun.free;

import static com.github.tonivade.purefun.Precondition.checkNonNull;

import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.annotation.HigherKind;

@HigherKind
public sealed interface ConsoleAlg<T> extends ConsoleAlgOf<T> {

  record WriteLine(String line) implements ConsoleAlg<Unit> {
    public WriteLine {
      checkNonNull(line);
    }
  }

  record ReadLine() implements ConsoleAlg<String> { }
}
