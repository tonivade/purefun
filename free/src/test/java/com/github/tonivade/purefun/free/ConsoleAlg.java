package com.github.tonivade.purefun.free;

import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Unit;

import static java.util.Objects.requireNonNull;

@HigherKind
public interface ConsoleAlg<T> {
  final class WriteLine implements ConsoleAlg<Unit> {
    final String line;
    WriteLine(String line) { this.line = requireNonNull(line); }
  }
  final class ReadLine implements ConsoleAlg<String> {
    ReadLine() { }
  }
}
