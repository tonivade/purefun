/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.optics;

import static com.github.tonivade.purefun.Function1.identity;
import static com.github.tonivade.purefun.data.ImmutableList.toImmutableList;
import static com.github.tonivade.purefun.data.Sequence.zipWithIndex;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.data.ImmutableList;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Option;

public class OptionalTest {

  private final Optional<ImmutableList<String>, String> optionalHead = Optional.of(
    (target, value) -> zipWithIndex(target).map(tuple -> tuple.get2() == 0 ? value : tuple.get1()).collect(toImmutableList()),
    target -> target.head()
  );

  private final ImmutableList<String> list12 = ImmutableList.of("1", "2");

  @Test
  public void optional() {
    assertAll(
      () -> assertEquals(Either.right("1"), optionalHead.getOrModify(list12)),
      () -> assertEquals(Either.left(ImmutableList.empty()), optionalHead.getOrModify(ImmutableList.empty())),
      () -> assertEquals(Option.some("1"), optionalHead.getOption(list12)),
      () -> assertEquals(Option.none(), optionalHead.getOption(ImmutableList.empty())),
      () -> assertEquals(ImmutableList.of("3", "2"), optionalHead.set(list12, "3")),
      () -> assertEquals(ImmutableList.empty(), optionalHead.set(ImmutableList.empty(), "3")),
      () -> assertEquals(ImmutableList.of("11", "2"), optionalHead.modify(list12, a -> a + a)),
      () -> assertEquals(ImmutableList.empty(), optionalHead.modify(ImmutableList.empty(), a -> a + a)),
      () -> assertEquals(Option.some(ImmutableList.of("11", "2")), optionalHead.modifyOption(list12, a -> a + a)),
      () -> assertEquals(Option.none(), optionalHead.modifyOption(ImmutableList.empty(), a -> a + a))
    );
  }

  @Test
  public void employee() {
    Optional<Employee, Address> addressOptional = Optional.of(
      Employee::withAddress, employee -> Option.of(employee::getAddress)
    );

    Address madrid = new Address("Madrid");
    Employee pepe = new Employee("pepe", null);

    assertEquals(Option.none(), addressOptional.getOption(pepe));
    assertEquals(Option.some(madrid), addressOptional.getOption(addressOptional.set(pepe, madrid)));
  }

  @Test
  public void optionalLaws() {
    verifyLaws(optionalHead, list12, "3");
  }

  private <S, A> void verifyLaws(Optional<S, A> optional, S target, A value) {
    assertAll(
      () -> assertEquals(target, optional.getOrModify(target).fold(identity(), a -> optional.set(target, a))),
      () -> assertEquals(optional.getOption(target).map(ignore -> value),
                         optional.getOption(optional.set(target, value)))
    );
  }
}
