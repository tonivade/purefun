/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.effect;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.effect.util.PureRandom;

public class ZRandomTest {

  @Test
  public void nextInt() {
    URIO<PureRandom, Integer> nextInt = PureRandom.nextInt();

    Integer provide = nextInt.unsafeRunSync(PureRandom.test(1L));

    assertEquals(-1155869325, provide);
  }

  @Test
  public void nextLong() {
    URIO<PureRandom, Long> nextLong = PureRandom.nextLong();

    Long provide = nextLong.unsafeRunSync(PureRandom.test(1L));

    assertEquals(-4964420948893066024L, provide);
  }

  @Test
  public void nextFloat() {
    URIO<PureRandom, Float> nextFloat = PureRandom.nextFloat();

    Float provide = nextFloat.unsafeRunSync(PureRandom.test(1L));

    assertEquals(0.7308782f, provide);
  }

  @Test
  public void nextDouble() {
    URIO<PureRandom, Double> nextDouble = PureRandom.nextDouble();

    Double provide = nextDouble.unsafeRunSync(PureRandom.test(1L));

    assertEquals(0.7308781907032909d, provide);
  }

  @Test
  public void nextChar() {
    URIO<PureRandom, Character> nextChar = PureRandom.nextChar();

    Character provide = nextChar.unsafeRunSync(PureRandom.test(1L));

    assertEquals('d', provide);
  }

  @Test
  public void nextString() {
    URIO<PureRandom, String> nextString = PureRandom.nextString(5);

    String provide = nextString.unsafeRunSync(PureRandom.test(1L));

    assertEquals("d0LpK", provide);
  }

  @Test
  public void nextStringLive() {
    URIO<PureRandom, String> nextString = PureRandom.nextString(5);

    String provide = nextString.unsafeRunSync(PureRandom.live());

    assertEquals(5, provide.length());
  }
}
