/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.zio;

import org.junit.jupiter.api.Test;

import static com.github.tonivade.purefun.zio.UIO.from;
import static com.github.tonivade.purefun.zio.UIO.pure;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class UIOTest {

  @Test
  public void mapRight() {
    Integer result = parseInt("1").map(x -> x + 1).run();

    assertEquals(2, result);
  }

  @Test
  public void mapLeft() {
    UIO<Integer> result = parseInt("lskjdf").map(x -> x + 1);

    assertThrows(NumberFormatException.class, result::run);
  }

  @Test
  public void flatMapRight() {
    Integer result = parseInt("1").flatMap(x -> pure(x + 1)).run();

    assertEquals(2, result);
  }

  @Test
  public void flatMapLeft() {
    UIO<Integer> result = parseInt("kjere").flatMap(x -> pure(x + 1));

    assertThrows(NumberFormatException.class, result::run);
  }

  private UIO<Integer> parseInt(String string) {
    return from(() -> Integer.parseInt(string));
  }
}
