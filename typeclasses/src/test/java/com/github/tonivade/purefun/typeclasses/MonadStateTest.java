/*
 * Copyright (c) 2018-2026, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.data.Sequence.arrayOf;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.data.ImmutableArray;
import com.github.tonivade.purefun.monad.IO;
import com.github.tonivade.purefun.monad.IOOf;

public class MonadStateTest {

  private MonadState<IO<?>, ImmutableArray<String>> monadState =
      MonadState.from(Instances.<IO<?>>monadDefer(), ImmutableArray.empty());

  @Test
  public void program() {
    IO<ImmutableArray<String>> result = For.with(monadState)
        .then(monadState.modify(list -> list.append("a")))
        .then(monadState.modify(list -> list.append("b")))
        .then(monadState.modify(list -> list.append("c")))
        .then(monadState.get()).fix(IOOf::toIO);

    assertEquals(arrayOf("a", "b", "c"), result.unsafeRunSync());
  }
}