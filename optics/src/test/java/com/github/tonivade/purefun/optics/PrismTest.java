/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.optics;

import static com.github.tonivade.purefun.core.Function1.identity;
import static com.github.tonivade.purefun.core.Producer.cons;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.type.Try;

public class PrismTest {

  private final Function1<String, Integer> parseInt = Integer::parseInt;
  private final Prism<Option<String>, String> someString = Prism.of(identity(), Option::some);
  private final Prism<String, Integer> stringToInteger = Prism.of(parseInt.liftTry().andThen(Try::toOption), String::valueOf);
  private final Prism<Option<String>, Integer> someStringToInteger = someString.compose(stringToInteger);

  @Test
  public void prismTest() {
    Option<String> some = Option.some("a");
    Option<String> none = Option.none();
    assertAll(
      () -> assertEquals(some, someString.getOption(some)),
      () -> assertEquals(none, someString.getOption(none)),
      () -> assertEquals(some, someString.reverseGet("a")),
      () -> assertEquals(Option.some("A"), someString.modify(String::toUpperCase).apply(some)),
      () -> assertEquals(none, someString.modify(String::toUpperCase).apply(none)),
      () -> assertEquals(Option.some("A"), someString.modifyOption(String::toUpperCase).apply(some).flatMap(identity())),
      () -> assertEquals(none, someString.modifyOption(String::toUpperCase).apply(none)),
      () -> assertEquals(Option.some("A"), someString.set("A").apply(some)),
      () -> assertEquals(none, someString.set("A").apply(none)),
      () -> assertEquals(Option.some("A"), someString.setOption("A").apply(some).flatMap(identity())),
      () -> assertEquals(none, someString.setOption("A").apply(none)),
      () -> assertEquals(Either.right("a"), someString.getOrModify(some)),
      () -> assertEquals(Either.left(none), someString.getOrModify(none))
    );
  }

  @Test
  public void prismComposition() {
    Option<String> some = Option.some("a");
    Option<String> five = Option.some("5");
    Option<String> none = Option.none();

    assertAll(
      () -> assertEquals(Option.some(5), someStringToInteger.getOption(five)),
      () -> assertEquals(none, someStringToInteger.getOption(none)),
      () -> assertEquals(five, someStringToInteger.reverseGet(5)),
      () -> assertEquals(Option.some("6"), someStringToInteger.modify(i -> i + 1).apply(five)),
      () -> assertEquals(none, someStringToInteger.modify(i -> i + 1).apply(none)),
      () -> assertEquals(Option.some("6"), someStringToInteger.modifyOption(i -> i + 1).apply(five).flatMap(identity())),
      () -> assertEquals(none, someStringToInteger.modifyOption(i -> i + 1).apply(none)),
      () -> assertEquals(Option.some("4"), someStringToInteger.set(4).apply(five)),
      () -> assertEquals(none, someStringToInteger.set(4).apply(none)),
      () -> assertEquals(Option.some("4"), someStringToInteger.setOption(4).apply(five).flatMap(identity())),
      () -> assertEquals(none, someStringToInteger.setOption(4).apply(none).flatMap(identity())),
      () -> assertEquals(some, someStringToInteger.set(4).apply(some)),
      () -> assertEquals(Either.right(5), someStringToInteger.getOrModify(five)),
      () -> assertEquals(Either.left(none), someString.getOrModify(none))
    );
  }

  @Test
  public void prismLaws() {
    verifyLaws(someString, Option.some("5"), "5");
    verifyLaws(stringToInteger, "5", 5);
    verifyLaws(someStringToInteger, Option.some("5"), 5);
  }

  private <S, A> void verifyLaws(Prism<S, A> prism, S target, A value) {
    assertAll(
      () -> assertTrue(prism.getOption(target).fold(cons(true), x -> x.equals(value))),
      () -> assertEquals(Option.some(value), prism.getOption(prism.reverseGet(value)))
    );
  }
}
