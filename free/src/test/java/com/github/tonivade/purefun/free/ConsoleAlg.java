package com.github.tonivade.purefun.free;

import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Unit;

import static com.github.tonivade.purefun.Precondition.checkNonNull;

@HigherKind
public interface ConsoleAlg<T> {
  final class WriteLine implements ConsoleAlg<Unit> {
    private final String line;
    public WriteLine(String line) { this.line = checkNonNull(line); }

    public String getLine() { return line; }
  }
  final class ReadLine implements ConsoleAlg<String> { }
}
