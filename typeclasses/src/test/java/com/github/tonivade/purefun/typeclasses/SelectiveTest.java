/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.core.Unit.unit;
import static com.github.tonivade.purefun.data.Sequence.listOf;
import static com.github.tonivade.purefun.monad.IOOf.toIO;
import static com.github.tonivade.purefun.type.ValidationOf.toValidation;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Producer;
import com.github.tonivade.purefun.core.Unit;
import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.instances.SequenceInstances;
import com.github.tonivade.purefun.instances.ValidationInstances;
import com.github.tonivade.purefun.monad.IO;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Validation;

@ExtendWith(MockitoExtension.class)
class SelectiveTest {

  private final Selective<IO<?>> monad = Instances.monad();

  @Test
  void apply() {
    Selective<Kind<Validation<?, ?>, Sequence<String>>> selective =
        ValidationInstances.selective(SequenceInstances.semigroup());

    var validValue = Validation.<Sequence<String>, Integer>valid(1);
    var invalidValue = Validation.<Sequence<String>, Integer>invalid(listOf("error 1"));
    var apply = Validation.<Sequence<String>, Function1<? super Integer, ? extends String>>valid(Function1.of(String::valueOf));
    var invalidApply = Validation.<Sequence<String>, Function1<? super Integer, ? extends String>>invalid(listOf("error 2"));

    assertEquals(Validation.valid("1"), selective.ap(validValue, apply).fix(toValidation()));
    assertEquals(Validation.invalid(listOf("error 1", "error 2")), selective.ap(invalidValue, invalidApply).fix(toValidation()));
    assertEquals(Validation.invalid(listOf("error 1")), selective.ap(invalidValue, apply).fix(toValidation()));
    assertEquals(Validation.invalid(listOf("error 2")), selective.ap(validValue, invalidApply).fix(toValidation()));
  }

  @Test
  void select() {
    Function1<String, Integer> parseInt = Integer::parseInt;

    var left = monad.select(monad.pure(Either.left("1")), monad.pure(parseInt));
    var right = monad.select(monad.pure(Either.right(-1)), monad.pure(parseInt));

    assertAll(
        () -> assertEquals(1, left.fix(toIO()).unsafeRunSync()),
        () -> assertEquals(-1, right.fix(toIO()).unsafeRunSync())
    );
  }

  @Test
  void branch() {
    Function1<String, Integer> parseInt = Integer::parseInt;
    Function1<String, Integer> countLetters = String::length;

    var left =
        monad.branch(monad.pure(Either.left("1")), monad.pure(parseInt), monad.pure(countLetters));
    var right =
        monad.branch(monad.pure(Either.right("asdfg")), monad.pure(parseInt), monad.pure(countLetters));

    assertAll(
        () -> assertEquals(1, left.fix(toIO()).unsafeRunSync()),
        () -> assertEquals(5, right.fix(toIO()).unsafeRunSync())
    );
  }

  @Test
  void whenS(@Mock Producer<Unit> left, @Mock Producer<Unit> right) {
    when(left.get()).thenReturn(unit());

    IO<Unit> io1 = IO.task(left);
    IO<Unit> io2 = IO.task(right);

    monad.whenS(monad.pure(true), io1).fix(toIO()).unsafeRunSync();
    monad.whenS(monad.pure(false), io2).fix(toIO()).unsafeRunSync();

    verify(left).get();
    verify(right, never()).get();
  }

  @Test
  void ifS() {
    var left = monad.ifS(monad.pure(true), monad.pure("left"), monad.pure("right"));
    var right = monad.ifS(monad.pure(false), monad.pure("left"), monad.pure("right"));

    assertAll(
        () -> assertEquals("left", left.fix(toIO()).unsafeRunSync()),
        () -> assertEquals("right", right.fix(toIO()).unsafeRunSync())
    );
  }

  @Test
  void andS() {
    var and00 = monad.andS(monad.pure(false), monad.pure(false));
    var and01 = monad.andS(monad.pure(false), monad.pure(true));
    var and10 = monad.andS(monad.pure(true), monad.pure(false));
    var and11 = monad.andS(monad.pure(true), monad.pure(true));

    assertAll(
        () -> assertEquals(false, and00.fix(toIO()).unsafeRunSync()),
        () -> assertEquals(false, and01.fix(toIO()).unsafeRunSync()),
        () -> assertEquals(false, and10.fix(toIO()).unsafeRunSync()),
        () -> assertEquals(true, and11.fix(toIO()).unsafeRunSync())
    );
  }

  @Test
  void orS() {
    var or00 = monad.orS(monad.pure(false), monad.pure(false));
    var or01 = monad.orS(monad.pure(false), monad.pure(true));
    var or10 = monad.orS(monad.pure(true), monad.pure(false));
    var or11 = monad.orS(monad.pure(true), monad.pure(true));

    assertAll(
        () -> assertEquals(false, or00.fix(toIO()).unsafeRunSync()),
        () -> assertEquals(true, or01.fix(toIO()).unsafeRunSync()),
        () -> assertEquals(true, or10.fix(toIO()).unsafeRunSync()),
        () -> assertEquals(true, or11.fix(toIO()).unsafeRunSync())
    );
  }

  @Test
  void allS() {
    var match = monad.<Sequence<?>, String>allS(
            listOf("a", "b", "c"), a -> monad.pure(a.length() == 1));
    var notMatch = monad.<Sequence<?>, String>allS(
            listOf("a", "b", "cd"), a -> monad.pure(a.length() == 1));

    assertAll(
        () -> assertEquals(true, match.value().fix(toIO()).unsafeRunSync()),
        () -> assertEquals(false, notMatch.value().fix(toIO()).unsafeRunSync())
    );
  }

  @Test
  void anyS() {
    var match = monad.<Sequence<?>, String>anyS(
            listOf("a", "b", "cd"), a -> monad.pure(a.length() > 1));
    var notMatch = monad.<Sequence<?>, String>anyS(
            listOf("a", "b", "c"), a -> monad.pure(a.length() > 1));

    assertAll(
        () -> assertEquals(true, match.value().fix(toIO()).unsafeRunSync()),
        () -> assertEquals(false, notMatch.value().fix(toIO()).unsafeRunSync())
    );
  }
}