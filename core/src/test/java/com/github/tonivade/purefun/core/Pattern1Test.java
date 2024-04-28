/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.core;

import static com.github.tonivade.purefun.data.Sequence.arrayOf;
import static com.github.tonivade.purefun.data.Sequence.listOf;
import static com.github.tonivade.purefun.data.Sequence.setOf;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.data.ImmutableArray;
import com.github.tonivade.purefun.data.ImmutableList;
import com.github.tonivade.purefun.data.ImmutableSet;

public class Pattern1Test {

  @Test
  public void instanceOf() {
    Matcher1<Object> isNumber = Matcher1.instanceOf(Number.class);
    Matcher1<Object> isString = Matcher1.instanceOf(String.class);

    Pattern1<Object, String> pattern = Pattern1.<Object, String>build()
      .when(isNumber).returns("is a number")
      .when(isString).returns("is a string")
      .otherwise().then(object -> "is something else: " + object.getClass().getSimpleName());

    assertAll(() -> assertEquals("is a number", pattern.apply(1)),
              () -> assertEquals("is a string", pattern.apply("1")),
              () -> assertEquals("is something else: Object", pattern.apply(new Object())),
              () -> assertTrue(pattern.isDefinedAt("any")));
  }

  @Test
  public void instanceOfWithTypeParams() {
    Pattern1<Object, String> pattern = Pattern1.<Object, String>build()
        .when(ImmutableList.class).then(list -> "is a list of size " + list.size())
        .when(ImmutableSet.class).returns("is a set")
        .when(ImmutableArray.class).returns("is an array")
        .otherwise().returns("something else");

    assertAll(() -> assertEquals("is a list of size 1", pattern.apply(listOf("1"))),
              () -> assertEquals("is a set", pattern.apply(setOf("a"))),
              () -> assertEquals("is an array", pattern.apply(arrayOf("a"))),
              () -> assertEquals("something else", pattern.apply(null)));
  }

  @Test
  public void is() {
    Pattern1<String, Boolean> pattern = Pattern1.<String, Boolean>build()
        .when(Matcher1.is("hola")).returns(true)
        .otherwise().returns(false);

    assertAll(() -> assertTrue(pattern.apply("hola")),
              () -> assertFalse(pattern.apply("hello")));
  }

  @Test
  public void isNot() {
    Pattern1<String, Boolean> pattern = Pattern1.<String, Boolean>build()
        .when(Matcher1.is("hola").negate()).returns(true)
        .otherwise().returns(false);

    assertAll(() -> assertTrue(pattern.apply("hello")),
              () -> assertFalse(pattern.apply("hola")));
  }

  @Test
  public void isIn() {
    Pattern1<String, Boolean> pattern = Pattern1.<String, Boolean>build()
        .when(Matcher1.isIn("hola", "hello", "ciao")).returns(true)
        .otherwise().returns(false);

    assertAll(() -> assertTrue(pattern.apply("hola")),
              () -> assertTrue(pattern.apply("hello")),
              () -> assertTrue(pattern.apply("ciao")),
              () -> assertFalse(pattern.apply("hi")));
  }

  @Test
  public void isNotNull() {
    Pattern1<String, Boolean> pattern = Pattern1.<String, Boolean>build()
        .when(Matcher1.isNotNull()).returns(true)
        .otherwise().returns(false);

    assertAll(() -> assertTrue(pattern.apply("hola")),
              () -> assertFalse(pattern.apply(null)));
  }

  @Test
  public void isNull() {
    Pattern1<String, Boolean> pattern = Pattern1.<String, Boolean>build()
        .when(Matcher1.isNull()).returns(true)
        .otherwise().returns(false);

    assertAll(() -> assertTrue(pattern.apply(null)),
              () -> assertFalse(pattern.apply("hello")));
  }

  @Test
  public void allOf() {
    Pattern1<String, Boolean> pattern = Pattern1.<String, Boolean>build()
        .when(Matcher1.allOf(Matcher1.isNotNull(), Matcher1.is("hola"))).returns(true)
        .otherwise().returns(false);

    assertAll(() -> assertTrue(pattern.apply("hola")),
              () -> assertFalse(pattern.apply("hello")));
  }

  @Test
  public void and() {
    Pattern1<String, Boolean> pattern = Pattern1.<String, Boolean>build()
        .when(Matcher1.<String>isNotNull().and(Matcher1.is("hola"))).returns(true)
        .otherwise().returns(false);

    assertAll(() -> assertTrue(pattern.apply("hola")),
              () -> assertFalse(pattern.apply("hello")));
  }

  @Test
  public void anyOf() {
    Pattern1<String, Boolean> pattern = Pattern1.<String, Boolean>build()
        .when(Matcher1.anyOf(Matcher1.is("hello"), Matcher1.is("hola"))).returns(true)
        .otherwise().returns(false);

    assertAll(() -> assertTrue(pattern.apply("hola")),
              () -> assertTrue(pattern.apply("hello")),
              () -> assertFalse(pattern.apply("ciao")));
  }

  @Test
  public void or() {
    Pattern1<String, Boolean> pattern = Pattern1.<String, Boolean>build()
        .when(Matcher1.is("hello").or(Matcher1.is("hola"))).returns(true)
        .otherwise().returns(false);

    assertAll(() -> assertTrue(pattern.apply("hola")),
              () -> assertTrue(pattern.apply("hello")),
              () -> assertFalse(pattern.apply("ciao")));
  }
}
