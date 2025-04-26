/*
 * Copyright (c) 2018-2025, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.transformer;

import static com.github.tonivade.purefun.data.Sequence.listOf;
import static com.github.tonivade.purefun.transformer.WriterT.lift;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.core.Tuple;
import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.instances.SequenceInstances;
import com.github.tonivade.purefun.type.Id;
import com.github.tonivade.purefun.typeclasses.Instances;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.Monoid;

public class WriterTTest {

  private final Monoid<Sequence<String>> monoid = SequenceInstances.monoid();
  private final Monad<Id<?>> monad = Instances.monad();

  @Test
  public void writerTest() {
    WriterT<Id<?>, Sequence<String>, Integer> writer =
        WriterT.<Id<?>, Sequence<String>, Integer>pure(monoid, monad, 5)
        .flatMap(value -> lift(monoid, monad, Tuple.of(listOf("add 5"), value + 5)))
        .flatMap(value -> lift(monoid, monad, Tuple.of(listOf("plus 2"), value * 2)));

    assertAll(() -> assertEquals(Id.of(Integer.valueOf(20)), writer.getValue()),
              () -> assertEquals(Id.of(listOf("add 5", "plus 2")), writer.getLog()));
  }
}
