package com.github.tonivade.purefun.monad;

import static com.github.tonivade.purefun.monad.Free.liftF;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.Higher;
import com.github.tonivade.purefun.Nothing;
import com.github.tonivade.purefun.Witness;

public class FreeTest {

  @Test
  public void echo() {
    Free<IOProgram.µ, Nothing> echo = IOProgram.read().flatMap(IOProgram::write);

    echo.foldMap(Nothing.nothing());
  }
}

interface IOProgram<T> extends Higher<IOProgram.µ, T> {
  final class µ implements Witness {}

  static Free<IOProgram.µ, String> read() {
    return liftF(new Read());
  }

  static Free<IOProgram.µ, Nothing> write(String value) {
    return liftF(new Write(value));
  }

  final class Read implements IOProgram<String> {
    private Read() {}
  }

  final class Write implements IOProgram<Nothing> {
    final String value;

    private Write(String value) {
      this.value = value;
    }
  }
}