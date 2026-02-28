/*
 * Copyright (c) 2018-2026, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.type.Option.some;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.type.Option;

public class TupleK2Test {

  @Test
  public void tuple() {
    TupleK2<Option<?>, String, Integer> tuple = TupleK.of(some("value"), some(10));

    assertAll(() -> assertEquals(TupleK.of(some("value"), some(10)), tuple),
              () -> assertEquals(TupleK.of(some("VALUE"), some(10)), tuple.map1(String::toUpperCase)),
              () -> assertEquals(TupleK.of(some("value"), some(100)), tuple.map2((i -> i * i))),
              () -> assertEquals(some("value"), tuple.get1()),
              () -> assertEquals(some(10), tuple.get2()),
              () -> assertEquals("TupleK2(Some(value),Some(10))", tuple.toString())
        );
  }
}
