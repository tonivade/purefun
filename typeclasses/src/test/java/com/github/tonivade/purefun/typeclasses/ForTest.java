/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Tuple;
import com.github.tonivade.purefun.Tuple5;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.instances.IOInstances;
import com.github.tonivade.purefun.instances.IdInstances;
import com.github.tonivade.purefun.monad.IO;
import com.github.tonivade.purefun.type.Id;
import org.junit.jupiter.api.Test;

import static com.github.tonivade.purefun.Unit.unit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ForTest {

  @Test
  public void map() {
    Id<String> result = For.with(IdInstances.monad())
        .andThen(() -> Id.of("value").kind1())
        .map(String::toUpperCase)
        .fix(Id::narrowK);

    assertEquals(Id.of("VALUE"), result);
  }

  @Test
  public void returns() {
    Function1<String, String> mapper = mock(Function1.class);
    when(mapper.apply(anyString())).thenReturn("called");
    when(mapper.andThen(any())).thenCallRealMethod();

    Higher1<Id.µ, Unit> result = For.with(IdInstances.monad())
        .and("hola mundo!")
        .map(mapper)
        .returns(unit());

    assertEquals(Id.of(unit()), result);
    verify(mapper).apply("hola mundo!");
  }

  @Test
  public void flatMap() {
    Monad<Id.µ> monad = IdInstances.monad();
    Id<String> result = For.with(monad)
        .andThen(() -> monad.pure("value"))
        .flatMap(string -> monad.pure(string.toUpperCase()))
        .fix(Id::narrowK);

    assertEquals(Id.of("VALUE"), result);
  }

  @Test
  public void apply() {
    Id<Tuple5<String, String, String, String, String>> result =
        For.with(IdInstances.monad())
          .and("a")
          .and("b")
          .and("c")
          .and("d")
          .and("e")
          .tuple()
          .fix1(Id::narrowK);

    assertEquals(Id.of(Tuple.of("a", "b", "c", "d", "e")), result);
  }

  @Test
  public void applyVsYield() {
    For5<IO.µ, Integer, Integer, Integer, Integer, Integer> program =
      For.with(IOInstances.monad())
        .and(1)
        .map(a -> 1 + a)
        .map(b -> 1 + b)
        .map(c -> 1 + c)
        .map(d -> 1 + d);

    IO<Integer> yield =
      program
        .yield((a, b, c, d, e) -> a + b + c + d + e).fix1(IO::narrowK);

    IO<Integer> apply =
      program
        .apply((a, b, c, d, e) -> a + b + c + d + e).fix1(IO::narrowK);

    assertEquals(15, yield.unsafeRunSync());
    assertEquals(15, apply.unsafeRunSync());
  }
}
