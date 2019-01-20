/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.With.with;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.type.Id;

public class DoMonadTest {

  @Test
  public void map() {
    Id<String> result = with("value")
      .lift(Id.monad())
      .map(String::toUpperCase)
      .get(Id::narrowK);

    assertEquals(Id.of("VALUE"), result);
  }

  @Test
  public void flatMap() {
    Id<String> result = with("value")
      .lift(Id.monad())
      .flatMap(string -> Id.of(string.toUpperCase()))
      .get(Id::narrowK);

    assertEquals(Id.of("VALUE"), result);
  }
}