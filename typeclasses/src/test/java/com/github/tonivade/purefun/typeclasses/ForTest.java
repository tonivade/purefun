/*
 * Copyright (c) 2018-2021, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.Unit.unit;
import static com.github.tonivade.purefun.monad.IOOf.toIO;
import static com.github.tonivade.purefun.type.IdOf.toId;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Tuple;
import com.github.tonivade.purefun.Tuple5;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.monad.IO;
import com.github.tonivade.purefun.monad.IO_;
import com.github.tonivade.purefun.type.Id;
import com.github.tonivade.purefun.type.Id_;

@ExtendWith(MockitoExtension.class)
public class ForTest {

  @Test
  public void map() {
    Id<String> result = For.with(Id_.class)
        .andThen(() -> Id.of("value"))
        .map(String::toUpperCase)
        .fix(toId());

    assertEquals(Id.of("VALUE"), result);
  }

  @Test
  public void returns(@Mock Function1<String, String> mapper) {
    when(mapper.apply(anyString())).thenReturn("called");
    when(mapper.andThen(any())).thenCallRealMethod();

    Kind<Id_, Unit> result = For.with(Id_.class)
        .and("hola mundo!")
        .map(mapper)
        .returns(unit());

    assertEquals(Id.of(unit()), result);
    verify(mapper).apply("hola mundo!");
  }

  @Test
  public void flatMap() {
    Monad<Id_> monad = Instances.<Id_>monad();
    Id<String> result = For.with(monad)
        .andThen(() -> monad.pure("value"))
        .flatMap(string -> monad.pure(string.toUpperCase()))
        .fix(toId());

    assertEquals(Id.of("VALUE"), result);
  }

  @Test
  public void apply() {
    Id<Tuple5<String, String, String, String, String>> result =
        For.with(Id_.class)
          .and("a")
          .and("b")
          .and("c")
          .and("d")
          .and("e")
          .tuple()
          .fix(toId());

    assertEquals(Id.of(Tuple.of("a", "b", "c", "d", "e")), result);
  }

  @Test
  public void applyVsYield() {
    For5<IO_, Integer, Integer, Integer, Integer, Integer> program =
      For.with(IO_.class)
        .and(1)
        .map(a -> 1 + a)
        .map(b -> 1 + b)
        .map(c -> 1 + c)
        .map(d -> 1 + d);

    IO<Integer> yield =
      program
        .yield((a, b, c, d, e) -> a + b + c + d + e).fix(toIO());

    IO<Integer> apply =
      program
        .apply((a, b, c, d, e) -> a + b + c + d + e).fix(toIO());

    assertEquals(15, yield.unsafeRunSync());
    assertEquals(15, apply.unsafeRunSync());
  }
}
