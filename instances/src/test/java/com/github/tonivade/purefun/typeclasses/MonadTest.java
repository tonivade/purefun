/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.laws.MonadLaws.verifyLaws;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.instances.EitherInstances;
import com.github.tonivade.purefun.instances.OptionInstances;
import com.github.tonivade.purefun.instances.ValidationInstances;
import com.github.tonivade.purefun.type.Id;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.type.Try;

public class MonadTest {

  private final Function1<String, String> toUpperCase = String::toUpperCase;

  @Test
  public void idMonad() {
    verifyLaws(Instances.<Id<?>>monad());
  }

  @Test
  public void optionMonad() {
    verifyLaws(Instances.<Option<?>>monad());
  }

  @Test
  public void tryMonad() {
    verifyLaws(Instances.<Try<?>>monad());
  }

  @Test
  public void eitherMonad() {
    verifyLaws(EitherInstances.monad());
  }

  @Test
  public void validationMonad() {
    verifyLaws(ValidationInstances.monad());
  }

  @Test
  public void option() {
    Monad<Option<?>> monad = OptionInstances.monad();

    Option<String> some = Option.some("asdf");
    Option<String> none = Option.none();

    assertAll(() -> assertEquals(some.map(toUpperCase), monad.map(some, toUpperCase)),
              () -> assertEquals(some.map(toUpperCase), monad.flatMap(some, toUpperCase.liftOption().andThen(Option::kind))),
              () -> assertEquals(some, monad.pure("asdf")),
              () -> assertEquals(none, monad.map(none, toUpperCase)),
              () -> assertEquals(none, monad.flatMap(none, toUpperCase.liftOption().andThen(Option::kind))),
              () -> assertEquals(some.map(toUpperCase), monad.ap(some, Option.some(toUpperCase))));
  }
}
