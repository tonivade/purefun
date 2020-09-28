/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.effect;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.effect.util.ZRandom;

public class ZRandomTest {

  @Test
  public void nextInt() {
    URIO<ZRandom, Integer> nextInt = ZRandom.nextInt();

    Integer provide = nextInt.unsafeRunSync(ZRandom.test(1L));

    assertEquals(-1155869325, provide);
  }

  @Test
  public void nextLong() {
    URIO<ZRandom, Long> nextLong = ZRandom.nextLong();

    Long provide = nextLong.unsafeRunSync(ZRandom.test(1L));

    assertEquals(-4964420948893066024L, provide);
  }

  @Test
  public void nextFloat() {
    URIO<ZRandom, Float> nextFloat = ZRandom.nextFloat();

    Float provide = nextFloat.unsafeRunSync(ZRandom.test(1L));

    assertEquals(0.7308782f, provide);
  }

  @Test
  public void nextDouble() {
    URIO<ZRandom, Double> nextDouble = ZRandom.nextDouble();

    Double provide = nextDouble.unsafeRunSync(ZRandom.test(1L));

    assertEquals(0.7308781907032909d, provide);
  }

  @Test
  public void nextChar() {
    URIO<ZRandom, Character> nextChar = ZRandom.nextChar();

    Character provide = nextChar.unsafeRunSync(ZRandom.test(1L));

    assertEquals('d', provide);
  }

  @Test
  public void nextString() {
    URIO<ZRandom, String> nextString = ZRandom.nextString(5);

    String provide = nextString.unsafeRunSync(ZRandom.test(1L));

    assertEquals("d0LpK", provide);
  }

  @Test
  public void nextStringLive() {
    URIO<ZRandom, String> nextString = ZRandom.nextString(5);

    String provide = nextString.unsafeRunSync(ZRandom.live());

    assertEquals(5, provide.length());
  }
}
