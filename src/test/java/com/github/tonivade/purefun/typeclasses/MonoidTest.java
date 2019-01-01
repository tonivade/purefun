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

public class MonoidTest {

  private final Monoid<Integer> monoid = new Monoid<Integer>() {

    @Override
    public Integer combine(Integer t1, Integer t2) {
      return t1 + t2;
    }

    @Override
    public Integer zero() {
      return 0;
    }
  };

  @TestFactory
  public Stream<DynamicNode> associativityLaw() {
    return IntStream.range(0, 10)
        .mapToObj(x -> dynamicTest("associativity law: " + x, this::associativity));
  }

  @TestFactory
  public Stream<DynamicNode> rightIdentityLaw() {
    return IntStream.range(0, 10)
        .mapToObj(x -> dynamicTest("associativity law: " + x, () -> rightIdentity(x)));
  }

  @TestFactory
  public Stream<DynamicNode> leftIdentityLaw() {
    return IntStream.range(0, 10)
        .mapToObj(x -> dynamicTest("associativity law: " + x, () -> leftIdentity(x)));
  }

  private void rightIdentity(Integer x) {
    assertEquals(x, monoid.combine(x, monoid.zero()));
  }

  private void leftIdentity(Integer x) {
    assertEquals(x, monoid.combine(monoid.zero(), x));
  }

  private void associativity() {
    Integer a = current().nextInt();
    Integer b = current().nextInt();
    Integer c = current().nextInt();
    assertEquals(monoid.combine(monoid.combine(a, b), c),
                 monoid.combine(a, monoid.combine(b, c)));
  }
}
