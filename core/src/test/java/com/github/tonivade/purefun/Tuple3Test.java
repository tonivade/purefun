/*
 * Copyright (c) 2018-2023, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.core.Tuple;
import com.github.tonivade.purefun.core.Tuple3;

public class Tuple3Test {
  
  @Test
  public void tuple() {
    Tuple3<String, Integer, LocalDate> tuple = Tuple.of("value", 10, LocalDate.of(2018, 11, 5));

    assertAll(() -> assertEquals(Tuple.of("value", 10, LocalDate.of(2018, 11, 5)), tuple),
              () -> assertEquals(Tuple.of("VALUE", 10, LocalDate.of(2018, 11, 5)), tuple.map1(String::toUpperCase)),
              () -> assertEquals(Tuple.of("value", 100, LocalDate.of(2018, 11, 5)), tuple.map2(i -> i * i)),
              () -> assertEquals(Tuple.of("value", 10, LocalDate.of(2018, 11, 6)), tuple.map3(date -> date.plusDays(1))),
              () -> assertEquals(Tuple.of("VALUE", 100, LocalDate.of(2018, 11, 6)), 
                  tuple.map(String::toUpperCase, i -> i * i, date -> date.plusDays(1))),
              () -> assertEquals("value", tuple.get1()),
              () -> assertEquals(Integer.valueOf(10), tuple.get2()),
              () -> assertEquals(LocalDate.of(2018, 11, 5), tuple.get3()),
              () -> assertEquals("Tuple3(value, 10, 2018-11-05)", tuple.toString())
        );
  }
}
