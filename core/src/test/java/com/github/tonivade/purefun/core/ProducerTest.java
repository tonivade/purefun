/*
 * Copyright (c) 2018-2025, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.core;

import static com.github.tonivade.purefun.core.Unit.unit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.Random;

import org.junit.jupiter.api.Test;

public class ProducerTest {

  @Test
  public void andThen() {
    Producer<String> producer = Producer.of(() -> "hello world");

    Producer<String> andThen = producer.map(String::toUpperCase);

    assertEquals("HELLO WORLD", andThen.get());
  }

  @Test
  public void unitTest() {
    assertEquals("hello world", Producer.cons("hello world").get());
  }

  @Test
  public void memoized() {
    Random random = new Random();
    Producer<Integer> nextInt = Producer.of(random::nextInt);
    Producer<Integer> nextIntMemoized = Producer.of(random::nextInt).memoized();

    assertNotEquals(nextInt.get(), nextInt.get());
    assertEquals(nextIntMemoized.get(), nextIntMemoized.get());
  }

  @Test
  public void asFunction() {
    Producer<String> producer = Producer.cons("hello world");

    assertEquals("hello world", producer.asFunction().apply(unit()));
  }
}
