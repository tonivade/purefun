/*
 * Copyright (c) 2018-2023, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
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
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Tuple;
import com.github.tonivade.purefun.monad.IO;
import com.github.tonivade.purefun.monad.IO_;
import com.github.tonivade.purefun.type.Id;
import com.github.tonivade.purefun.type.Id_;

@ExtendWith(MockitoExtension.class)
public class ForTest {

  final Monad<Id_> monad = Instances.<Id_>monad();

  @Test
  public void map() {
    Id<String> result = monad.use()
        .andThen(() -> Id.of("value"))
        .map(String::toUpperCase)
        .fix(toId());

    assertEquals(Id.of("VALUE"), result);
  }

  @Test
  public void returns(@Mock Function1<String, String> mapper) {
    when(mapper.apply(anyString())).thenReturn("called");
    when(mapper.andThen(any())).thenCallRealMethod();

    var result = monad.use()
        .and("hola mundo!")
        .map(mapper)
        .returns(unit());

    assertEquals(Id.of(unit()), result);
    verify(mapper).apply("hola mundo!");
  }

  @Test
  public void flatMap() {
    Id<String> result = For.with(Instances.<Id_>monad())
        .andThen(() -> Instances.<Id_>monad().pure("value"))
        .flatMap(string -> Instances.<Id_>monad().pure(string.toUpperCase()))
        .fix(toId());

    assertEquals(Id.of("VALUE"), result);
  }

  @Test
  public void applyBug(@Mock Producer<String> task1, @Mock Producer<String> task2) {
    when(task1.get()).thenReturn("hola toni");
    when(task2.get()).thenReturn("adios toni");

    Applicative<IO_> monad = Instances.applicative(IO_.class);
    var result = For.with(monad)
      .then(IO.task(task1))
      .then(IO.task(task2))
      .apply(String::concat)
      .fix(toIO())
      .unsafeRunSync();

    assertEquals("hola toniadios toni", result);
    verify(task1).get();
    verify(task2).get();
  }

  @Test
  public void apply() {
    var result = monad.use()
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
    var program1 = For.with(Instances.<IO_>monad())
        .and(1)
        .and(2)
        .and(3)
        .and(4)
        .and(5);
    var program2 = For.with(Instances.<IO_>applicative())
        .and(1)
        .and(2)
        .and(3)
        .and(4)
        .and(5);

    IO<Integer> yield = program1.apply((a, b, c, d, e) -> a + b + c + d + e).fix(toIO());

    IO<Integer> apply = program2.apply((a, b, c, d, e) -> a + b + c + d + e).fix(toIO());

    assertEquals(15, yield.unsafeRunSync());
    assertEquals(15, apply.unsafeRunSync());
  }
}
