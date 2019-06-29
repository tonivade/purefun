/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.Tuple;
import com.github.tonivade.purefun.Tuple5;
import com.github.tonivade.purefun.instances.IdInstances;
import com.github.tonivade.purefun.type.Id;

public class ForTest {

  @Test
  public void map() {
    Id<String> result = For.with(IdInstances.monad())
        .andThen(() -> Id.of("value"))
        .map(String::toUpperCase)
        .fix(Id::narrowK);

    assertEquals(Id.of("VALUE"), result);
  }

  @Test
  public void flatMap() {
    Id<String> result = For.with(IdInstances.monad())
        .andThen(() -> Id.of("value"))
        .flatMap(string -> Id.of(string.toUpperCase()))
        .fix(Id::narrowK);

    assertEquals(Id.of("VALUE"), result);
  }

  @Test
  public void yield() {
    Id<Tuple5<String, String, String, String, String>> result =
        For.with(IdInstances.monad())
          .andThen(() -> Id.of("a"))
          .andThen(() -> Id.of("b"))
          .andThen(() -> Id.of("c"))
          .andThen(() -> Id.of("d"))
          .andThen(() -> Id.of("e"))
          .tuple()
          .fix1(Id::narrowK);

    assertEquals(Id.of(Tuple.of("a", "b", "c", "d", "e")), result);
  }
}
