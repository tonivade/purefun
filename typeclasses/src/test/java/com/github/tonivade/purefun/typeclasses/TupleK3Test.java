/*
 * Copyright (c) 2018-2021, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.type.Option.some;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.type.Option_;

public class TupleK3Test {

  private final Functor<Option_> functor = Instance.functor(Option_.class);
  
  @Test
  public void tuple() {

    TupleK3<Option_, String, Integer, LocalDate> tuple = TupleK.of(some("value"), some(10), some(LocalDate.of(2018, 11, 5)));

    assertAll(() -> assertEquals(TupleK.of(some("value"), some(10), some(LocalDate.of(2018, 11, 5))), tuple),
              () -> assertEquals(TupleK.of(some("VALUE"), some(10), some(LocalDate.of(2018, 11, 5))), tuple.map1(functor, String::toUpperCase)),
              () -> assertEquals(TupleK.of(some("value"), some(100), some(LocalDate.of(2018, 11, 5))), tuple.map2(functor, i -> i * i)),
              () -> assertEquals(TupleK.of(some("value"), some(10), some(LocalDate.of(2018, 11, 6))), tuple.map3(functor, date -> date.plusDays(1))),
              () -> assertEquals(some("value"), tuple.get1()),
              () -> assertEquals(some(10), tuple.get2()),
              () -> assertEquals(some(LocalDate.of(2018, 11, 5)), tuple.get3()),
              () -> assertEquals("TupleK3(Some(value),Some(10),Some(2018-11-05))", tuple.toString())
        );
  }
}