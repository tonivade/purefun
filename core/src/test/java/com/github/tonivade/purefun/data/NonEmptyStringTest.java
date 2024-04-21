/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class NonEmptyStringTest {

  @Test
  public void nonEmptyString() {
    NonEmptyString nonEmptyString = NonEmptyString.of("hola mundo");

    assertAll(
        () -> assertThrows(IllegalArgumentException.class, () -> NonEmptyString.of(null)),
        () -> assertThrows(IllegalArgumentException.class, () -> NonEmptyString.of("")),
        () -> assertDoesNotThrow(() -> NonEmptyString.of("hola mundo")),
        () -> assertEquals("hola mundo", nonEmptyString.get()),
        () -> assertEquals("HOLA MUNDO", nonEmptyString.transform(String::toUpperCase)),
        () -> assertEquals(NonEmptyString.of("HOLA MUNDO"), nonEmptyString.map(String::toUpperCase)),
        () -> assertEquals(NonEmptyString.of("hola mundo"), NonEmptyString.of("hola mundo"))
    );
  }
}