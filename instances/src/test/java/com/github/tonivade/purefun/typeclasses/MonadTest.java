/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.laws.MonadLaws.verifyLaws;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.instances.EitherInstances;
import com.github.tonivade.purefun.instances.IdInstances;
import com.github.tonivade.purefun.instances.OptionInstances;
import com.github.tonivade.purefun.instances.TryInstances;
import com.github.tonivade.purefun.instances.ValidationInstances;
import com.github.tonivade.purefun.type.Option;

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
}
