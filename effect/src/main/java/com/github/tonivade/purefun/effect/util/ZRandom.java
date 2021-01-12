/*
 * Copyright (c) 2018-2021, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.effect.util;

import static com.github.tonivade.purefun.Precondition.checkNonNull;
import static java.util.stream.Collectors.joining;

import java.util.Random;
import java.util.stream.IntStream;

import com.github.tonivade.purefun.effect.URIO;

public interface ZRandom {

  <R extends ZRandom> ZRandom.Service<R> random();

  static <R extends ZRandom> URIO<R, Integer> nextInt() {
    return URIO.accessM(env -> env.<R>random().nextInt());
  }

  static <R extends ZRandom> URIO<R, Long> nextLong() {
    return URIO.accessM(env -> env.<R>random().nextLong());
  }

  static <R extends ZRandom> URIO<R, Float> nextFloat() {
    return URIO.accessM(env -> env.<R>random().nextFloat());
  }

  static <R extends ZRandom> URIO<R, Double> nextDouble() {
    return URIO.accessM(env -> env.<R>random().nextDouble());
  }

  static <R extends ZRandom> URIO<R, Character> nextChar() {
    return URIO.accessM(env -> env.<R>random().nextChar());
  }

  static <R extends ZRandom> URIO<R, String> nextString(int length) {
    return URIO.accessM(env -> env.<R>random().nextString(length));
  }

  interface Service<R extends ZRandom> {
    URIO<R, Integer> nextInt();
    URIO<R, Long> nextLong();
    URIO<R, Float> nextFloat();
    URIO<R, Double> nextDouble();
    URIO<R, Character> nextChar();
    URIO<R, String> nextString(int length);
  }

  static ZRandom live() {
    return new ZRandomImpl(new Random());
  }

  static ZRandom test(long seed) {
    return new ZRandomImpl(new Random(seed));
  }
}

class ZRandomImpl implements ZRandom {

  private static final String PRINTABLE_CHARS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

  private final Random random;

  ZRandomImpl(Random random) {
    this.random = checkNonNull(random);
  }

  @Override
  public <R extends ZRandom> ZRandom.Service<R> random() {
    return new ZRandom.Service<R>() {

      @Override
      public URIO<R, Integer> nextInt() {
        return URIO.task(random::nextInt);
      }

      @Override
      public URIO<R, Long> nextLong() {
        return URIO.task(random::nextLong);
      }

      @Override
      public URIO<R, Float> nextFloat() {
        return URIO.task(random::nextFloat);
      }

      @Override
      public URIO<R, Double> nextDouble() {
        return URIO.task(random::nextDouble);
      }

      @Override
      public URIO<R, Character> nextChar() {
        return URIO.task(this::randomChar);
      }

      @Override
      public URIO<R, String> nextString(int length) {
        return URIO.task(() -> randomString(length));
      }

      private Character randomChar() {
        return PRINTABLE_CHARS.charAt(random.nextInt(PRINTABLE_CHARS.length()));
      }

      private String randomString(int length) {
        return IntStream.range(0, length).mapToObj(x -> randomChar()).map(Object::toString).collect(joining());
      }
    };
  }
}
