/*
 * Copyright (c) 2018-2023, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.effect.util;

import static com.github.tonivade.purefun.Precondition.checkNonNull;
import static java.util.stream.Collectors.joining;

import java.util.Random;
import java.util.stream.IntStream;

import com.github.tonivade.purefun.effect.URIO;

public interface PureRandom {

  <R extends PureRandom> PureRandom.Service<R> random();

  static <R extends PureRandom> URIO<R, Integer> nextInt() {
    return URIO.accessM(env -> env.<R>random().nextInt());
  }

  static <R extends PureRandom> URIO<R, Long> nextLong() {
    return URIO.accessM(env -> env.<R>random().nextLong());
  }

  static <R extends PureRandom> URIO<R, Float> nextFloat() {
    return URIO.accessM(env -> env.<R>random().nextFloat());
  }

  static <R extends PureRandom> URIO<R, Double> nextDouble() {
    return URIO.accessM(env -> env.<R>random().nextDouble());
  }

  static <R extends PureRandom> URIO<R, Character> nextChar() {
    return URIO.accessM(env -> env.<R>random().nextChar());
  }

  static <R extends PureRandom> URIO<R, String> nextString(int length) {
    return URIO.accessM(env -> env.<R>random().nextString(length));
  }

  interface Service<R extends PureRandom> {
    URIO<R, Integer> nextInt();
    URIO<R, Long> nextLong();
    URIO<R, Float> nextFloat();
    URIO<R, Double> nextDouble();
    URIO<R, Character> nextChar();
    URIO<R, String> nextString(int length);
  }

  static PureRandom live() {
    return new PureRandomImpl(new Random());
  }

  static PureRandom test(long seed) {
    return new PureRandomImpl(new Random(seed));
  }
}

class PureRandomImpl implements PureRandom {

  private static final String PRINTABLE_CHARS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

  private final Random random;

  PureRandomImpl(Random random) {
    this.random = checkNonNull(random);
  }

  @Override
  public <R extends PureRandom> PureRandom.Service<R> random() {
    return new PureRandom.Service<>() {

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
