/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.Unit.unit;
import static com.github.tonivade.purefun.laws.MonadLaws.verifyLaws;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.instances.IOInstances;
import com.github.tonivade.purefun.monad.IO;
import com.github.tonivade.purefun.type.Either;
import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.instances.EitherInstances;
import com.github.tonivade.purefun.instances.IdInstances;
import com.github.tonivade.purefun.instances.OptionInstances;
import com.github.tonivade.purefun.instances.TryInstances;
import com.github.tonivade.purefun.instances.ValidationInstances;
import com.github.tonivade.purefun.type.Option;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public class MonadTest {

  @Test
  public void idMonad() {
    verifyLaws(IdInstances.monad());
  }

  @Test
  public void optionMonad() {
    verifyLaws(OptionInstances.monad());
  }

  @Test
  public void tryMonad() {
    verifyLaws(TryInstances.monad());
  }

  @Test
  public void eitherMonad() {
    verifyLaws(EitherInstances.monad());
  }

  @Test
  public void validationMonad() {
    verifyLaws(ValidationInstances.monad());
  }

  private final Function1<String, String> toUpperCase = string -> string.toUpperCase();

  @Test
  public void option() {
    Monad<Option.µ> monad = OptionInstances.monad();

    Option<String> some = Option.some("asdf");
    Option<String> none = Option.none();

    assertAll(() -> assertEquals(some.map(toUpperCase), monad.map(some.kind1(), toUpperCase)),
              () -> assertEquals(some.map(toUpperCase), monad.flatMap(some.kind1(), toUpperCase.liftOption().andThen(Option::kind1))),
              () -> assertEquals(some, monad.pure("asdf")),
              () -> assertEquals(none, monad.map(none.kind1(), toUpperCase)),
              () -> assertEquals(none, monad.flatMap(none.kind1(), toUpperCase.liftOption().andThen(Option::kind1))),
              () -> assertEquals(some.map(toUpperCase), monad.ap(some.kind1(), Option.some(toUpperCase).kind1())));
  }

  @Test
  public void select() {
    Monad<IO.µ> monad = IOInstances.monad();

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
    Monad<IO.µ> monad = IOInstances.monad();

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
  public void when() {
    Monad<IO.µ> monad = IOInstances.monad();

    Producer<Unit> left = mock(Producer.class);
    Producer<Unit> right = mock(Producer.class);
    Mockito.when(left.get()).thenReturn(unit());
    Mockito.when(right.get()).thenReturn(unit());
    Mockito.when(left.map(ArgumentMatchers.any())).thenCallRealMethod();
    Mockito.when(right.map(ArgumentMatchers.any())).thenCallRealMethod();

    IO<Unit> io1 = IO.delay(left);
    IO<Unit> io2 = IO.delay(right);

    monad.when(monad.pure(true), io1.kind1()).fix1(IO::narrowK).unsafeRunSync();
    monad.when(monad.pure(false), io2.kind1()).fix1(IO::narrowK).unsafeRunSync();

    verify(left).get();
    verify(right, never()).get();
  }
}
