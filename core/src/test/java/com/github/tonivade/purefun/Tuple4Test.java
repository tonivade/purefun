/*
 * Copyright (c) 2018-2022, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import static com.github.tonivade.purefun.Unit.unit;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

public class Tuple4Test {

  @Test
  public void tuple() {
    Tuple4<String, Integer, LocalDate, Unit> tuple =
        Tuple.of("value", 10, LocalDate.of(2018, 11, 5), unit());

    assertAll(() -> assertEquals(Tuple.of("value", 10, LocalDate.of(2018, 11, 5), unit()), tuple),
              () -> assertEquals(Tuple.of("VALUE", 10, LocalDate.of(2018, 11, 5), unit()), tuple.map1(String::toUpperCase)),
              () -> assertEquals(Tuple.of("value", 100, LocalDate.of(2018, 11, 5), unit()), tuple.map2(i -> i * i)),
              () -> assertEquals(Tuple.of("value", 10, LocalDate.of(2018, 11, 6), unit()), tuple.map3(date -> date.plusDays(1))),
              () -> assertEquals(Tuple.of("value", 10, LocalDate.of(2018, 11, 5), "Unit"), tuple.map4(Object::toString)),
              () -> assertEquals(Tuple.of("VALUE", 100, LocalDate.of(2018, 11, 6), "Unit"),
                  tuple.map(String::toUpperCase, i -> i * i, date -> date.plusDays(1), Object::toString)),
              () -> assertEquals("value", tuple.get1()),
              () -> assertEquals(Integer.valueOf(10), tuple.get2()),
              () -> assertEquals(LocalDate.of(2018, 11, 5), tuple.get3()),
              () -> assertEquals(unit(), tuple.get4()),
              () -> assertEquals("Tuple4(value, 10, 2018-11-05, Unit)", tuple.toString())
        );
  }
}
