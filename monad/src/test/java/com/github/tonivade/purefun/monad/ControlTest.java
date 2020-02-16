/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.monad;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.data.ImmutableList;
import org.junit.jupiter.api.Test;

import static com.github.tonivade.purefun.data.Sequence.listOf;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ControlTest {

  @Test
  public void test() {
    ImmutableList<Integer> handled = ambList(this::program).run();

    assertEquals(listOf(2, 3), handled);
  }

  private Control<Integer> program(Amb amb) {
    return amb.flip().map(x -> x ? 2 : 3);
  }

  private <R> Control<ImmutableList<R>> ambList(Function1<Amb, Control<R>> program) {
    return new AmbList<R>().<AmbList<R>>apply(amb -> program.apply(amb).map(ImmutableList::of));
  }

  interface Amb {
    Control<Boolean> flip();
  }

  static final class AmbList<R> implements Handler<ImmutableList<R>>, Amb {

    @Override
    public Control<Boolean> flip() {
      return use(resume ->
          resume.apply(true).flatMap(ts -> resume.apply(false).map(ts::appendAll)));
    }
  }
}