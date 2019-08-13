/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.optics;

import com.github.tonivade.purefun.data.ImmutableList;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Option;
import org.junit.jupiter.api.Test;

import static com.github.tonivade.purefun.data.ImmutableList.toImmutableList;
import static com.github.tonivade.purefun.data.Sequence.zipWithIndex;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class OptionalTest {

  private final Optional<ImmutableList<String>, String> optionalHead = Optional.of(
    (target, value) -> zipWithIndex(target).map(tuple -> tuple.get2() == 0 ? value : tuple.get1()).collect(toImmutableList()),
    target -> target.head().fold(() -> Either.left(target), Either::right)
  );

  @Test
  public void optional() {
    assertAll(
      () -> assertEquals(Either.right("1"), optionalHead.getOrModify(ImmutableList.of("1", "2"))),
      () -> assertEquals(Either.left(ImmutableList.empty()), optionalHead.getOrModify(ImmutableList.empty())),
      () -> assertEquals(Option.some("1"), optionalHead.getOption(ImmutableList.of("1", "2"))),
      () -> assertEquals(Option.none(), optionalHead.getOption(ImmutableList.empty())),
      () -> assertEquals(ImmutableList.of("3", "2"), optionalHead.set(ImmutableList.of("1", "2"), "3")),
      () -> assertEquals(ImmutableList.empty(), optionalHead.set(ImmutableList.empty(), "3")),
      () -> assertEquals(ImmutableList.of("11", "2"), optionalHead.modify(ImmutableList.of("1", "2"), a -> a + a)),
      () -> assertEquals(ImmutableList.empty(), optionalHead.modify(ImmutableList.empty(), a -> a + a)),
      () -> assertEquals(Option.some(ImmutableList.of("11", "2")), optionalHead.modifyOption(ImmutableList.of("1", "2"), a -> a + a)),
      () -> assertEquals(Option.none(), optionalHead.modifyOption(ImmutableList.empty(), a -> a + a))
    );
  }
}
