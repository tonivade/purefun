package com.github.tonivade.purefun.typeclasses;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.Tuple;
import com.github.tonivade.purefun.instances.OptionInstances;
import com.github.tonivade.purefun.type.Option;

public class SemigroupalTest {

  @Test
  public void semigroupal() {
    Semigroupal<Option.µ> instance = OptionInstances.semigroupal();

    assertAll(
        () -> assertEquals(Option.none(), instance.product(Option.none(), Option.none())),
        () -> assertEquals(Option.none(), instance.product(Option.some(1), Option.none())),
        () -> assertEquals(Option.none(), instance.product(Option.none(), Option.some("a"))),
        () -> assertEquals(Option.some(Tuple.of(1, "a")), instance.product(Option.some(1), Option.some("a"))));
  }
}
