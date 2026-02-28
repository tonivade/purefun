/*
 * Copyright (c) 2018-2026, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static java.util.concurrent.ThreadLocalRandom.current;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

public class MonoidTest {

  private final Monoid<Integer> intMonoid = Monoid.integer();

  @Test
  public void invariant() {
    Monoid<String> strMonoid = intMonoid.imap(String::valueOf, Integer::parseInt);

    assertEquals("0", strMonoid.zero());
    assertEquals("3", strMonoid.combine("1", "2"));
  }

  @TestFactory
  public Stream<DynamicNode> associativityLaw() {
    return IntStream.range(0, 10)
        .mapToObj(x -> dynamicTest("associativity law: " + x, this::associativity));
  }

  @TestFactory
  public Stream<DynamicNode> rightIdentityLaw() {
    return IntStream.range(0, 10)
        .mapToObj(x -> dynamicTest("right identity law: " + x, () -> rightIdentity(x)));
  }

  @TestFactory
  public Stream<DynamicNode> leftIdentityLaw() {
    return IntStream.range(0, 10)
        .mapToObj(x -> dynamicTest("left identity law: " + x, () -> leftIdentity(x)));
  }

  private void rightIdentity(Integer x) {
    assertEquals(x, intMonoid.combine(x, intMonoid.zero()));
  }

  private void leftIdentity(Integer x) {
    assertEquals(x, intMonoid.combine(intMonoid.zero(), x));
  }

  private void associativity() {
    Integer a = current().nextInt();
    Integer b = current().nextInt();
    Integer c = current().nextInt();
    assertEquals(intMonoid.combine(intMonoid.combine(a, b), c),
                 intMonoid.combine(a, intMonoid.combine(b, c)));
  }
}
