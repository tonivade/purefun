/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.zio.util;

import com.github.tonivade.purefun.Nothing;
import com.github.tonivade.purefun.zio.ZIO;

import java.util.Random;
import java.util.stream.IntStream;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

public interface ZRandom {

  <R extends ZRandom> ZRandom.Service<R> random();

  static ZIO<ZRandom, Nothing, Integer> nextInt() {
    return ZIO.accessM(env -> env.random().nextInt());
  }

  static ZIO<ZRandom, Nothing, Long> nextLong() {
    return ZIO.accessM(env -> env.random().nextLong());
  }

  static ZIO<ZRandom, Nothing, Float> nextFloat() {
    return ZIO.accessM(env -> env.random().nextFloat());
  }

  static ZIO<ZRandom, Nothing, Double> nextDouble() {
    return ZIO.accessM(env -> env.random().nextDouble());
  }

  static ZIO<ZRandom, Nothing, Character> nextChar() {
    return ZIO.accessM(env -> env.random().nextChar());
  }

  static ZIO<ZRandom, Nothing, String> nextString(int length) {
    return ZIO.accessM(env -> env.random().nextString(length));
  }

  interface Service<R extends ZRandom> {
    ZIO<R, Nothing, Integer> nextInt();
    ZIO<R, Nothing, Long> nextLong();
    ZIO<R, Nothing, Float> nextFloat();
    ZIO<R, Nothing, Double> nextDouble();
    ZIO<R, Nothing, Character> nextChar();
    ZIO<R, Nothing, String> nextString(int length);
  }

  static ZRandom live() {
    return new ZRandomImpl(new Random());
  }

  static ZRandom test(long seed) {
    return new ZRandomImpl(new Random(seed));
  }
}

class ZRandomImpl implements ZRandom {

  private final String printableChars = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

  private Random current;

  ZRandomImpl(Random current) {
    this.current = requireNonNull(current);
  }

  @Override
  public <R extends ZRandom> ZRandom.Service<R> random() {
    return new ZRandom.Service<R>() {

      @Override
      public ZIO<R, Nothing, Integer> nextInt() {
        return ZIO.task(current::nextInt);
      }

      @Override
      public ZIO<R, Nothing, Long> nextLong() {
        return ZIO.task(current::nextLong);
      }

      @Override
      public ZIO<R, Nothing, Float> nextFloat() {
        return ZIO.task(current::nextFloat);
      }

      @Override
      public ZIO<R, Nothing, Double> nextDouble() {
        return ZIO.task(current::nextDouble);
      }

      @Override
      public ZIO<R, Nothing, Character> nextChar() {
        return ZIO.task(() -> randomChar());
      }

      @Override
      public ZIO<R, Nothing, String> nextString(int length) {
        return ZIO.task(() -> IntStream.range(0, length).mapToObj(x -> randomChar()).map(Object::toString).collect(joining()));
      }
    };
  }

  private char randomChar() {
    return printableChars.charAt(current.nextInt(printableChars.length()));
  }
}
