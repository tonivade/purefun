/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.effect;

import com.github.tonivade.purefun.Nothing;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.effect.util.ZRandom;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ZRandomTest {

  @Test
  public void nextInt() {
    ZIO<ZRandom, Nothing, Integer> nextInt = ZRandom.nextInt();

    Either<Nothing, Integer> provide = nextInt.provide(ZRandom.test(1L));

    assertEquals(Either.right(-1155869325), provide);
  }

  @Test
  public void nextLong() {
    ZIO<ZRandom, Nothing, Long> nextLong = ZRandom.nextLong();

    Either<Nothing, Long> provide = nextLong.provide(ZRandom.test(1L));

    assertEquals(Either.right(-4964420948893066024L), provide);
  }

  @Test
  public void nextFloat() {
    ZIO<ZRandom, Nothing, Float> nextFloat = ZRandom.nextFloat();

    Either<Nothing, Float> provide = nextFloat.provide(ZRandom.test(1L));

    assertEquals(Either.right(0.7308782f), provide);
  }

  @Test
  public void nextDouble() {
    ZIO<ZRandom, Nothing, Double> nextDouble = ZRandom.nextDouble();

    Either<Nothing, Double> provide = nextDouble.provide(ZRandom.test(1L));

    assertEquals(Either.right(0.7308781907032909d), provide);
  }

  @Test
  public void nextChar() {
    ZIO<ZRandom, Nothing, Character> nextChar = ZRandom.nextChar();

    Either<Nothing, Character> provide = nextChar.provide(ZRandom.test(1L));

    assertEquals(Either.right('d'), provide);
  }

  @Test
  public void nextString() {
    ZIO<ZRandom, Nothing, String> nextString = ZRandom.nextString(5);

    Either<Nothing, String> provide = nextString.provide(ZRandom.test(1L));

    assertEquals(Either.right("d0LpK"), provide);
  }

  @Test
  public void nextStringLive() {
    ZIO<ZRandom, Nothing, String> nextString = ZRandom.nextString(5);

    Either<Nothing, String> provide = nextString.provide(ZRandom.live());

    assertEquals(Either.right(5), provide.map(String::length));
  }
}
