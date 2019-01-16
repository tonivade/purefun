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
import static com.github.tonivade.purefun.typeclasses.FoldableLaws.verifyLaws;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.Nested;
import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Id;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.type.Try;

public class FoldableTest {

  @Test
  public void laws() {
    assertAll(
        () -> verifyLaws(Id.foldable(), Id.of("hola")),
        () -> verifyLaws(Try.foldable(), Try.success("hola")),
        () -> verifyLaws(Either.foldable(), Either.right("hola")),
        () -> verifyLaws(Option.foldable(), Option.some("hola")),
        () -> verifyLaws(Sequence.foldable(), Sequence.listOf("hola")),
        () -> verifyLaws(compose(Sequence.foldable(), Option.foldable()), nest(listOf(Option.some("hola")))));
  }

  @Test
  public void composed() {
    Foldable<Nested<Sequence.µ, Option.µ>> instance = compose(Sequence.foldable(), Option.foldable());

    assertEquals(Integer.valueOf(3), instance.fold(Monoid.integer(), nest(listOf(some(1), none(), some(2)))));
  }
}