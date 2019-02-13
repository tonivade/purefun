/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.typeclasses.MonadLaws.verifyLaws;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.instances.EitherInstances;
import com.github.tonivade.purefun.instances.OptionInstances;
import com.github.tonivade.purefun.instances.TryInstances;
import com.github.tonivade.purefun.type.Id;
import com.github.tonivade.purefun.type.Validation;

public class MonadTest {

  @Test
  public void idMonad() {
    verifyLaws(Id.monad());
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
    verifyLaws(Validation.monad());
  }
}
