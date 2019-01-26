package com.github.tonivade.purefun.stream;

import static com.github.tonivade.purefun.type.Eval.now;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.monad.IO;
import com.github.tonivade.purefun.monad.IO.µ;
import com.github.tonivade.purefun.type.Eval;
import com.github.tonivade.purefun.typeclasses.Comonad;
import com.github.tonivade.purefun.typeclasses.Monad;

public class StreamTest {

  final Monad<µ> monad = IO.monad();
  final Comonad<µ> comonad = IO.comonad();

  @Test
  public void map() {
    Stream<IO.µ, String> pure1 = Stream.pure(monad, comonad, "hola");
    Stream<IO.µ, String> pure2 = Stream.pure(monad, comonad, " mundo");

    Stream<IO.µ, String> result = pure1.concat(pure2).map(String::toUpperCase);

    Eval<IO<String>> foldRight = result.foldRight(now(""), (a, b) -> b.map(x -> x + a)).map(IO::narrowK);

    assertEquals("HOLA MUNDO", foldRight.value().unsafeRunSync());
  }

  @Test
  public void flatMap() {
    Stream<IO.µ, String> pure1 = Stream.pure(monad, comonad, "hola");
    Stream<IO.µ, String> pure2 = Stream.pure(monad, comonad, " mundo");

    Stream<IO.µ, String> result = pure1.concat(pure2)
        .flatMap(string -> Stream.pure(monad, comonad, string.toUpperCase()));

    IO<String> foldLeft = result.foldLeft("", (a, b) -> a + b).fix1(IO::narrowK);

    assertEquals("HOLA MUNDO", foldLeft.unsafeRunSync());
  }

}
