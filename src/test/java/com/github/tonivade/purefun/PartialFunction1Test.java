/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import static com.github.tonivade.purefun.data.Sequence.arrayOf;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.data.ImmutableMap;
import com.github.tonivade.purefun.type.Option;

public class PartialFunction1Test {

  final Function1<String, String> toUpperCase = String::toUpperCase;
  final Function1<String, String> toLowerCase = String::toLowerCase;

  final PartialFunction1<Integer, String> fromArray =
      PartialFunction1.from(arrayOf("a", "b", "c"));
  final PartialFunction1<String, Integer> fromMap =
      PartialFunction1.from(ImmutableMap.<String, Integer>builder().put("a", 1).build());

  @Test
  public void lift() {
    Function1<Integer, Option<String>> function = fromArray.lift();

    assertAll(
        () -> assertEquals(Option.some("a"), function.apply(0)),
        () -> assertEquals(Option.none(), function.apply(10)));
  }

  @Test
  public void andThen() {
    PartialFunction1<Integer, String> function = fromArray.andThen(toUpperCase);

    assertAll(
        () -> assertTrue(function.isDefinedAt(0)),
        () -> assertFalse(function.isDefinedAt(10)));
  }

  @Test
  public void compose() {
    Function1<String, Integer> function = fromMap.compose(toLowerCase);

    assertAll(
        () -> assertEquals(Integer.valueOf(1), function.apply("A")),
        () -> assertThrows(NoSuchElementException.class, () -> function.apply("Z")));
  }

  @Test
  public void orElse() {
    PartialFunction1<Integer, String> function =
        fromArray.orElse(PartialFunction1.of(t -> "5", t -> t == 5));

    assertAll(
        () -> assertEquals("a", function.apply(0)),
        () -> assertEquals("5", function.apply(5)));
  }

  @Test
  public void applyOrElse() {
    assertAll(
        () -> assertEquals("a", fromArray.applyOrElse(0, t -> "not found")),
        () -> assertEquals("not found", fromArray.applyOrElse(10, t -> "not found")));
  }
}
