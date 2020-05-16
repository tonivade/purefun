/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.data.ImmutableArray;
import com.github.tonivade.purefun.instances.IOInstances;
import com.github.tonivade.purefun.monad.IO;
import com.github.tonivade.purefun.monad.IO_;
import org.junit.jupiter.api.Test;

import static com.github.tonivade.purefun.data.Sequence.arrayOf;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class MonadStateTest {

  private MonadState<IO_, ImmutableArray<String>> monadState =
      MonadState.from(IOInstances.monadDefer(), ImmutableArray.empty());

  @Test
  public void program() {
    IO<ImmutableArray<String>> result = For.with(monadState)
        .then(monadState.modify(list -> list.append("a")))
        .then(monadState.modify(list -> list.append("b")))
        .then(monadState.modify(list -> list.append("c")))
        .then(monadState.get()).fix(IO_::narrowK);

    assertEquals(arrayOf("a", "b", "c"), result.unsafeRunSync());
  }
}