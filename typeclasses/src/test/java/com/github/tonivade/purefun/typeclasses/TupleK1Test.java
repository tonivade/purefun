/*
 * Copyright (c) 2018-2023, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.type.Option.some;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.type.Option_;

@ExtendWith(MockitoExtension.class)
public class TupleK1Test {
  
  @Test
  public void tuple() {
    TupleK1<Option_, String> tuple = TupleK.of(some("value"));

    assertAll(
      () -> assertEquals(TupleK.of(some("value")), tuple),
      () -> assertEquals(TupleK.of(some("VALUE")), tuple.map1(String::toUpperCase)),
      () -> assertEquals(some("value"), tuple.get1()),
      () -> assertEquals("TupleK1(Some(value))", tuple.toString())
    );
  }

  @Test
  public void forEach(@Mock Consumer1<Object> callback) {
    TupleK1<Option_, String> tuple = TupleK.of(some("value"));

    tuple.forEach(callback);

    verify(callback).accept(some("value"));
  }
}
