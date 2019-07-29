/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.typeclasses.Monoid.integer;
import static com.github.tonivade.purefun.typeclasses.Monoid.string;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class MonoidInvariantTest {

  @Test
  public void invariant() {
    MonoidInvariant invariant = new MonoidInvariant() {};

    Monoid<String> monoid = invariant.imap(integer(), String::valueOf, Integer::parseInt);

    assertEquals("0", monoid.zero());
    assertEquals("3", monoid.combine("1", "2"));
  }
}
