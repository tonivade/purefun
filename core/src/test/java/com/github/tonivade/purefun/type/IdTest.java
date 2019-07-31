/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.type;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.Function1;

public class IdTest {

  private final Function1<String, String> toUpperCase = String::toUpperCase;

  @Test
  public void idTest() {
    Id<String> id = Id.of("hola mundo!");

    assertAll(
        () -> assertEquals("hola mundo!", id.get()),
        () -> assertEquals(Id.of("HOLA MUNDO!"), id.map(toUpperCase)),
        () -> assertEquals(Id.of("HOLA MUNDO!"), id.flatMap(toUpperCase.andThen(Id::of))));
  }
}
