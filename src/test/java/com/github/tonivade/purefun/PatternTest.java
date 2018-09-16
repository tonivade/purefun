/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

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
import com.github.tonivade.purefun.data.Sequence;

public class PatternTest {

  @Test
  public void instanceOf() {
    Matcher<Object> isNumber = Matcher.instanceOf(Number.class);
    Matcher<Object> isString = Matcher.instanceOf(String.class);

    Pattern<Object, String> pattern = Pattern.<Object, String>build()
      .when(isNumber).returns("is number")
      .when(isString).returns("is string")
      .otherwise().then(object -> "something else");


    assertAll(() -> assertEquals("is number", pattern.apply(1)),
              () -> assertEquals("is string", pattern.apply("1")),
              () -> assertEquals("something else", pattern.apply(null)));
  }

  @Test
  public void instanceOfWithTypeParams() {
    Matcher<Sequence<?>> isList = Matcher.instanceOf(ImmutableList.class);
    Matcher<Sequence<?>> isSet = Matcher.instanceOf(ImmutableSet.class);
    Matcher<Sequence<?>> isArray = Matcher.instanceOf(ImmutableArray.class);

    Pattern<Sequence<?>, String> pattern = Pattern.<Sequence<?>, String>build()
        .when(isList).returns("is a list")
        .when(isSet).returns("is a set")
        .when(isArray).returns("is an array")
        .otherwise().returns("something else");

    assertAll(() -> assertEquals("is a list", pattern.apply(listOf("1"))),
              () -> assertEquals("is a set", pattern.apply(setOf("a"))),
              () -> assertEquals("is an array", pattern.apply(arrayOf("a"))),
              () -> assertEquals("something else", pattern.apply(null)));
  }

  @Test
  public void is() {
    Pattern<String, Boolean> pattern = Pattern.<String, Boolean>build()
        .when(Matcher.is("hola")).returns(true)
        .otherwise().returns(false);

    assertAll(() -> assertTrue(pattern.apply("hola")),
              () -> assertFalse(pattern.apply("hello")));
  }

  @Test
  public void isNot() {
    Pattern<String, Boolean> pattern = Pattern.<String, Boolean>build()
        .when(Matcher.is("hola").negate()).returns(true)
        .otherwise().returns(false);

    assertAll(() -> assertTrue(pattern.apply("hello")),
              () -> assertFalse(pattern.apply("hola")));
  }

  @Test
  public void isIn() {
    Pattern<String, Boolean> pattern = Pattern.<String, Boolean>build()
        .when(Matcher.isIn("hola", "hello", "ciao")).returns(true)
        .otherwise().returns(false);

    assertAll(() -> assertTrue(pattern.apply("hola")),
              () -> assertTrue(pattern.apply("hello")),
              () -> assertTrue(pattern.apply("ciao")),
              () -> assertFalse(pattern.apply("hi")));
  }

  @Test
  public void isNotNull() {
    Pattern<String, Boolean> pattern = Pattern.<String, Boolean>build()
        .when(Matcher.isNotNull()).returns(true)
        .otherwise().returns(false);

    assertAll(() -> assertTrue(pattern.apply("hola")),
              () -> assertFalse(pattern.apply(null)));
  }

  @Test
  public void isNull() {
    Pattern<String, Boolean> pattern = Pattern.<String, Boolean>build()
        .when(Matcher.isNull()).returns(true)
        .otherwise().returns(false);

    assertAll(() -> assertTrue(pattern.apply(null)),
              () -> assertFalse(pattern.apply("hello")));
  }

  @Test
  public void allOf() {
    Pattern<String, Boolean> pattern = Pattern.<String, Boolean>build()
        .when(Matcher.allOf(Matcher.isNotNull(), Matcher.is("hola"))).returns(true)
        .otherwise().returns(false);

    assertAll(() -> assertTrue(pattern.apply("hola")),
              () -> assertFalse(pattern.apply("hello")));
  }
  @Test
  public void and() {
    Pattern<String, Boolean> pattern = Pattern.<String, Boolean>build()
        .when(Matcher.<String>isNotNull().and(Matcher.is("hola"))).returns(true)
        .otherwise().returns(false);

    assertAll(() -> assertTrue(pattern.apply("hola")),
              () -> assertFalse(pattern.apply("hello")));
  }

  @Test
  public void anyOf() {
    Pattern<String, Boolean> pattern = Pattern.<String, Boolean>build()
        .when(Matcher.anyOf(Matcher.is("hello"), Matcher.is("hola"))).returns(true)
        .otherwise().returns(false);

    assertAll(() -> assertTrue(pattern.apply("hola")),
              () -> assertTrue(pattern.apply("hello")),
              () -> assertFalse(pattern.apply("ciao")));
  }

  @Test
  public void or() {
    Pattern<String, Boolean> pattern = Pattern.<String, Boolean>build()
        .when(Matcher.is("hello").or(Matcher.is("hola"))).returns(true)
        .otherwise().returns(false);

    assertAll(() -> assertTrue(pattern.apply("hola")),
              () -> assertTrue(pattern.apply("hello")),
              () -> assertFalse(pattern.apply("ciao")));
  }
}
