package com.github.tonivade.purefun.monad;

import com.github.tonivade.purefun.Higher;
import com.github.tonivade.purefun.Nothing;
import com.github.tonivade.purefun.Witness;

import org.junit.jupiter.api.Test;

public class FreeTest {

  @Test
  public void test() {
    Free<IOProgram.µ, Nothing> echo = Free.liftF(new Read()).flatMap(x -> Free.liftF(new Write(x)));

    echo.foldMap(Nothing.nothing());
  }
}

interface IOProgram<T> extends Higher<IOProgram.µ, T> {
  final class µ implements Witness {}
}

final class Read implements IOProgram<String> {
}

final class Write implements IOProgram<Nothing> {
  final String value;

  public Write(String value) {
    this.value = value;
  }
}