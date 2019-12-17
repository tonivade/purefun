/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.instances.IOInstances;
import com.github.tonivade.purefun.instances.SequenceInstances;
import com.github.tonivade.purefun.instances.ValidationInstances;
import com.github.tonivade.purefun.monad.IO;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Eval;
import com.github.tonivade.purefun.type.Validation;
import org.junit.jupiter.api.Test;

import static com.github.tonivade.purefun.Unit.unit;
import static com.github.tonivade.purefun.data.Sequence.listOf;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SelectiveTest {

  private final Selective<IO.µ> monad = IOInstances.monad();

  @Test
  public void apply() {
    Selective<Higher1<Validation.µ, Sequence<String>>> selective =
        ValidationInstances.selective(SequenceInstances.semigroup());

    Higher1<Higher1<Validation.µ, Sequence<String>>, Integer> validValue =
        Validation.<Sequence<String>, Integer>valid(1).kind1();
    Higher1<Higher1<Validation.µ, Sequence<String>>, Integer> invalidValue =
        Validation.<Sequence<String>, Integer>invalid(listOf("error 1")).kind1();
    Higher1<Higher1<Validation.µ, Sequence<String>>, Function1<Integer, String>> apply =
        Validation.<Sequence<String>, Function1<Integer, String>>valid(Function1.of(String::valueOf)).kind1();
    Higher1<Higher1<Validation.µ, Sequence<String>>, Function1<Integer, String>> invalidApply =
        Validation.<Sequence<String>, Function1<Integer, String>>invalid(listOf("error 2")).kind1();

    assertEquals(Validation.valid("1"), selective.ap(validValue, apply).fix1(Validation::narrowK));
    assertEquals(Validation.invalid(listOf("error 1", "error 2")), selective.ap(invalidValue, invalidApply).fix1(Validation::narrowK));
    assertEquals(Validation.invalid(listOf("error 1")), selective.ap(invalidValue, apply).fix1(Validation::narrowK));
    assertEquals(Validation.invalid(listOf("error 2")), selective.ap(validValue, invalidApply).fix1(Validation::narrowK));
  }

  @Test
  public void select() {
    Function1<String, Integer> parseInt = Integer::parseInt;

    Higher1<IO.µ, Integer> left = monad.select(monad.pure(Either.left("1")), monad.pure(parseInt));
    Higher1<IO.µ, Integer> right = monad.select(monad.pure(Either.right(-1)), monad.pure(parseInt));

    assertAll(
        () -> assertEquals(1, left.fix1(IO::narrowK).unsafeRunSync()),
        () -> assertEquals(-1, right.fix1(IO::narrowK).unsafeRunSync())
    );
  }

  @Test
  public void branch() {
    Function1<String, Integer> parseInt = Integer::parseInt;
    Function1<String, Integer> countLetters = String::length;

    Higher1<IO.µ, Integer> left = monad.branch(monad.pure(Either.left("1")), monad.pure(parseInt), monad.pure(countLetters));
    Higher1<IO.µ, Integer> right = monad.branch(monad.pure(Either.right("asdfg")), monad.pure(parseInt), monad.pure(countLetters));

    assertAll(
        () -> assertEquals(1, left.fix1(IO::narrowK).unsafeRunSync()),
        () -> assertEquals(5, right.fix1(IO::narrowK).unsafeRunSync())
    );
  }

  @Test
  public void whenS() {
    Producer<Unit> left = mock(Producer.class);
    Producer<Unit> right = mock(Producer.class);
    when(left.get()).thenReturn(unit());
    when(right.get()).thenReturn(unit());

    IO<Unit> io1 = IO.task(left);
    IO<Unit> io2 = IO.task(right);

    monad.whenS(monad.pure(true), io1.kind1()).fix1(IO::narrowK).unsafeRunSync();
    monad.whenS(monad.pure(false), io2.kind1()).fix1(IO::narrowK).unsafeRunSync();

    verify(left).get();
    verify(right, never()).get();
  }

  @Test
  public void ifS() {
    Higher1<IO.µ, String> left = monad.ifS(monad.pure(true), monad.pure("left"), monad.pure("right"));
    Higher1<IO.µ, String> right = monad.ifS(monad.pure(false), monad.pure("left"), monad.pure("right"));

    assertAll(
        () -> assertEquals("left", left.fix1(IO::narrowK).unsafeRunSync()),
        () -> assertEquals("right", right.fix1(IO::narrowK).unsafeRunSync())
    );
  }

  @Test
  public void andS() {
    Higher1<IO.µ, Boolean> and00 = monad.andS(monad.pure(false), monad.pure(false));
    Higher1<IO.µ, Boolean> and01 = monad.andS(monad.pure(false), monad.pure(true));
    Higher1<IO.µ, Boolean> and10 = monad.andS(monad.pure(true), monad.pure(false));
    Higher1<IO.µ, Boolean> and11 = monad.andS(monad.pure(true), monad.pure(true));

    assertAll(
        () -> assertEquals(false, and00.fix1(IO::narrowK).unsafeRunSync()),
        () -> assertEquals(false, and01.fix1(IO::narrowK).unsafeRunSync()),
        () -> assertEquals(false, and10.fix1(IO::narrowK).unsafeRunSync()),
        () -> assertEquals(true, and11.fix1(IO::narrowK).unsafeRunSync())
    );
  }

  @Test
  public void orS() {
    Higher1<IO.µ, Boolean> or00 = monad.orS(monad.pure(false), monad.pure(false));
    Higher1<IO.µ, Boolean> or01 = monad.orS(monad.pure(false), monad.pure(true));
    Higher1<IO.µ, Boolean> or10 = monad.orS(monad.pure(true), monad.pure(false));
    Higher1<IO.µ, Boolean> or11 = monad.orS(monad.pure(true), monad.pure(true));

    assertAll(
        () -> assertEquals(false, or00.fix1(IO::narrowK).unsafeRunSync()),
        () -> assertEquals(true, or01.fix1(IO::narrowK).unsafeRunSync()),
        () -> assertEquals(true, or10.fix1(IO::narrowK).unsafeRunSync()),
        () -> assertEquals(true, or11.fix1(IO::narrowK).unsafeRunSync())
    );
  }

  @Test
  public void allS() {
    Eval<Higher1<IO.µ, Boolean>> match =
        monad.allS(SequenceInstances.foldable(),
            listOf("a", "b", "c").kind1(), a -> monad.pure(a.length() == 1));
    Eval<Higher1<IO.µ, Boolean>> notMatch =
        monad.allS(SequenceInstances.foldable(),
            listOf("a", "b", "cd").kind1(), a -> monad.pure(a.length() == 1));

    assertAll(
        () -> assertEquals(true, match.value().fix1(IO::narrowK).unsafeRunSync()),
        () -> assertEquals(false, notMatch.value().fix1(IO::narrowK).unsafeRunSync())
    );
  }

  @Test
  public void anyS() {
    Eval<Higher1<IO.µ, Boolean>> match =
        monad.anyS(SequenceInstances.foldable(),
            listOf("a", "b", "cd").kind1(), a -> monad.pure(a.length() > 1));
    Eval<Higher1<IO.µ, Boolean>> notMatch =
        monad.anyS(SequenceInstances.foldable(),
            listOf("a", "b", "c").kind1(), a -> monad.pure(a.length() > 1));

    assertAll(
        () -> assertEquals(true, match.value().fix1(IO::narrowK).unsafeRunSync()),
        () -> assertEquals(false, notMatch.value().fix1(IO::narrowK).unsafeRunSync())
    );
  }
}