/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import static com.github.tonivade.purefun.Matcher.otherwise;
import static com.github.tonivade.purefun.data.Sequence.arrayOf;
import static com.github.tonivade.purefun.data.Sequence.listOf;
import static com.github.tonivade.purefun.data.Sequence.setOf;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
      .when(isNumber).then(number -> "is number")
      .when(isString).then(string -> "is string")
      .when(otherwise()).then(object -> "something else");


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
        .when(isList).then(list -> "is a list")
        .when(isSet).then(set -> "is a set")
        .when(isArray).then(array -> "is an array")
        .when(otherwise()).then(object -> "something else");

    assertAll(() -> assertEquals("is a list", pattern.apply(listOf("1"))),
              () -> assertEquals("is a set", pattern.apply(setOf("a"))),
              () -> assertEquals("is an array", pattern.apply(arrayOf("a"))),
              () -> assertEquals("something else", pattern.apply(null)));
  }
}
