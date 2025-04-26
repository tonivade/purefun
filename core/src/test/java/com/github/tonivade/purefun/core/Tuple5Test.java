/*
 * Copyright (c) 2018-2025, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.core;

import static com.github.tonivade.purefun.core.Unit.unit;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

public class Tuple5Test {

  @Test
  public void tuple() {
    Tuple5<String, Integer, LocalDate, Unit, Double> tuple =
        Tuple.of("value", 10, LocalDate.of(2018, 11, 5), unit(), 1.0);

    assertAll(() -> assertEquals(Tuple.of("value", 10, LocalDate.of(2018, 11, 5), unit(), 1.0), tuple),
              () -> assertEquals(Tuple.of("VALUE", 10, LocalDate.of(2018, 11, 5), unit(), 1.0), tuple.map1(String::toUpperCase)),
              () -> assertEquals(Tuple.of("value", 100, LocalDate.of(2018, 11, 5), unit(), 1.0), tuple.map2(i -> i * i)),
              () -> assertEquals(Tuple.of("value", 10, LocalDate.of(2018, 11, 6), unit(), 1.0), tuple.map3(date -> date.plusDays(1))),
              () -> assertEquals(Tuple.of("value", 10, LocalDate.of(2018, 11, 5), "Unit", 1.0), tuple.map4(Object::toString)),
              () -> assertEquals(Tuple.of("value", 10, LocalDate.of(2018, 11, 5), unit(), 2.0), tuple.map5(d -> d + d)),
              () -> assertEquals(Tuple.of("VALUE", 100, LocalDate.of(2018, 11, 6), "Unit", 2.0),
                  tuple.map(String::toUpperCase, i -> i * i, date -> date.plusDays(1), Object::toString, d -> d + d)),
              () -> assertEquals("value", tuple.get1()),
              () -> assertEquals(Integer.valueOf(10), tuple.get2()),
              () -> assertEquals(LocalDate.of(2018, 11, 5), tuple.get3()),
              () -> assertEquals(unit(), tuple.get4()),
              () -> assertEquals(Double.valueOf(1.0), tuple.get5()),
              () -> assertEquals("Tuple5(value, 10, 2018-11-05, Unit, 1.0)", tuple.toString())
        );
  }

}
