/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.Nested.nest;
import static com.github.tonivade.purefun.data.Sequence.listOf;
import static com.github.tonivade.purefun.type.Option.none;
import static com.github.tonivade.purefun.type.Option.some;
import static com.github.tonivade.purefun.typeclasses.Foldable.compose;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.Nested;
import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.type.Option;

public class FoldableTest {

  @Test
  public void composed() {
    Foldable<Nested<Sequence.µ, Option.µ>> instance = compose(Sequence.foldable(), Option.foldable());

    assertEquals(Integer.valueOf(3), instance.fold(Monoid.integer(), nest(listOf(some(1), none(), some(2)))));
  }
}
