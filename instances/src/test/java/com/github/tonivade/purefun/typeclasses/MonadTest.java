/*
 * Copyright (c) 2018-2021, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
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
import com.github.tonivade.purefun.type.Option_;

public class MonadTest {

  private final Function1<String, String> toUpperCase = String::toUpperCase;

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

  @Test
  public void option() {
    Monad<Option_> monad = OptionInstances.monad();

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
