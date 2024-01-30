/*
 * Copyright (c) 2018-2023, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.type.Option.none;
import static com.github.tonivade.purefun.type.Option.some;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.type.Option_;

public class TupleK5Test {

  @Test
  public void tuple() {
    TupleK5<Option_, String, Integer, LocalDate, Unit, Double> tuple =
        TupleK.of(some("value"), some(10), some(LocalDate.of(2018, 11, 5)), none(), some(1.0));

    assertAll(() -> assertEquals(TupleK.of(some("value"), some(10), some(LocalDate.of(2018, 11, 5)), none(), some(1.0)), tuple),
              () -> assertEquals(TupleK.of(some("VALUE"), some(10), some(LocalDate.of(2018, 11, 5)), none(), some(1.0)), tuple.map1(String::toUpperCase)),
              () -> assertEquals(TupleK.of(some("value"), some(100), some(LocalDate.of(2018, 11, 5)), none(), some(1.0)), tuple.map2(i -> i * i)),
              () -> assertEquals(TupleK.of(some("value"), some(10), some(LocalDate.of(2018, 11, 6)), none(), some(1.0)), tuple.map3(date -> date.plusDays(1))),
              () -> assertEquals(TupleK.of(some("value"), some(10), some(LocalDate.of(2018, 11, 5)), none(), some(1.0)), tuple.map4(Object::toString)),
              () -> assertEquals(TupleK.of(some("value"), some(10), some(LocalDate.of(2018, 11, 5)), none(), some(2.0)), tuple.map5(d -> d + d)),
              () -> assertEquals(some("value"), tuple.get1()),
              () -> assertEquals(some(10), tuple.get2()),
              () -> assertEquals(some(LocalDate.of(2018, 11, 5)), tuple.get3()),
              () -> assertEquals(none(), tuple.get4()),
              () -> assertEquals(some(1.0), tuple.get5()),
              () -> assertEquals("TupleK5(Some(value),Some(10),Some(2018-11-05),None,Some(1.0))", tuple.toString())
        );
  }

}
