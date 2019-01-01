/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static java.util.concurrent.ThreadLocalRandom.current;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;

public class SemigroupTest {

  private final Semigroup<Integer> semigroup = (a, b) -> a + b;

  @TestFactory
  public Stream<DynamicNode> associativityLaw() {
    return IntStream.range(0, 10)
        .mapToObj(x -> dynamicTest("associativity law: " + x, this::test));
  }

  private void test() {
    Integer a = current().nextInt();
    Integer b = current().nextInt();
    Integer c = current().nextInt();
    assertEquals(semigroup.combine(semigroup.combine(a, b), c),
                 semigroup.combine(a, semigroup.combine(b, c)));
  }
}
