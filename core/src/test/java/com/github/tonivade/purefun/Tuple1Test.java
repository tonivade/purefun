/*
 * Copyright (c) 2018-2023, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.tonivade.purefun.core.Consumer1;
import com.github.tonivade.purefun.core.Tuple;
import com.github.tonivade.purefun.core.Tuple1;

@ExtendWith(MockitoExtension.class)
public class Tuple1Test {
  
  @Test
  public void tuple() {
    Tuple1<String> tuple = Tuple.of("value");

    assertAll(
      () -> assertEquals(Tuple.of("value"), tuple),
      () -> assertEquals(Tuple.of("VALUE"), tuple.map1(String::toUpperCase)),
      () -> assertEquals("value", tuple.get1()),
      () -> assertEquals("Tuple1(value)", tuple.toString())
    );
  }

  @Test
  public void forEach(@Mock Consumer1<Object> callback) {
    Tuple1<String> tuple = Tuple.of("value");

    tuple.forEach(callback);

    verify(callback).accept("value");
  }
}
