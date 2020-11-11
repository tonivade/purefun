/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.data.Sequence.arrayOf;
import static com.github.tonivade.purefun.monad.IOOf.toIO;
import static com.github.tonivade.purefun.typeclasses.Instance.monadDefer;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.data.ImmutableArray;
import com.github.tonivade.purefun.monad.IO;
import com.github.tonivade.purefun.monad.IO_;

public class MonadStateTest {

  private MonadState<IO_, ImmutableArray<String>> monadState =
      MonadState.from(monadDefer(IO_.class), ImmutableArray.empty());

  @Test
  public void program() {
    IO<ImmutableArray<String>> result = For.with(monadState)
        .then(monadState.modify(list -> list.append("a")))
        .then(monadState.modify(list -> list.append("b")))
        .then(monadState.modify(list -> list.append("c")))
        .then(monadState.get()).fix(toIO());

    assertEquals(arrayOf("a", "b", "c"), result.unsafeRunSync());
  }
}