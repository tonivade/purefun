/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

import static com.github.tonivade.zeromock.core.Function1.identity;
import static com.github.tonivade.zeromock.core.Producer.unit;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class PrismTest {

  private final Prism<Option<String>, String> someString = Prism.of(identity(), Option::some);
  private final Prism<String, Integer> stringToInteger =
        Prism.of(Function1.<String, Integer>of(Integer::parseInt).liftTry().toOption(), String::valueOf);
  private final Prism<Option<String>, Integer> someStringToInteger = someString.compose(stringToInteger);

  @Test
  public void prismTest() {
    Option<String> some = Option.some("a");
    Option<String> none = Option.none();

    assertAll(() -> assertEquals(some, someString.getOption(some)),
              () -> assertEquals(some, someString.reverseGet("a")),
              () -> assertEquals(Option.some("A"), someString.modify(some, String::toUpperCase)),
              () -> assertEquals(Option.some("A"), someString.modifyOption(some, String::toUpperCase).flatten()),
              () -> assertEquals(Option.some("A"), someString.set(some, "A")),
              () -> assertEquals(Option.some("A"), someString.setOption(some, "A").flatten()),
              () -> assertEquals(Either.right("a"), someString.getOrModify(some)),
              () -> assertEquals(Either.left(none), someString.getOrModify(none))
        );
  }

  @Test
  public void prismComposition() {
    Option<String> some = Option.some("a");
    Option<String> five = Option.some("5");
    Option<String> none = Option.none();

    assertAll(() -> assertEquals(Option.some(5), someStringToInteger.getOption(five)),
              () -> assertEquals(five, someStringToInteger.reverseGet(5)),
              () -> assertEquals(Option.some("6"), someStringToInteger.modify(five, i -> i + 1)),
              () -> assertEquals(Option.some("6"), someStringToInteger.modifyOption(five, i -> i + 1).flatten()),
              () -> assertEquals(Option.some("4"), someStringToInteger.set(five, 4)),
              () -> assertEquals(Option.some("4"), someStringToInteger.setOption(five, 4).flatten()),
              () -> assertEquals(some, someStringToInteger.set(some, 4)),
              () -> assertEquals(Either.right(5), someStringToInteger.getOrModify(five)),
              () -> assertEquals(Either.left(none), someString.getOrModify(none))
        );
  }

  @Test
  public void prismLaws() {
    assertAll(() -> assertTrue(stringToInteger.getOption("5").fold(unit(true), x -> x.equals(5))),
              () -> assertEquals(Option.some(5), stringToInteger.getOption(stringToInteger.reverseGet(5))));
  }
}
