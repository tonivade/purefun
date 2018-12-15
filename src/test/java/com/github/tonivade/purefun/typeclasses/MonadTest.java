/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.typeclasses.MonadLaws.verifyLaws;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.type.Try;
import com.github.tonivade.purefun.type.Validation;

public class MonadTest {

  @Test
  public void optionMonad() {
    verifyLaws(Option.monad(), Option.some("hola mundo"));
  }

  @Test
  public void tryMonad() {
    verifyLaws(Try.monad(), Try.success("hola mundo"));
  }

  @Test
  public void eitherMonad() {
    verifyLaws(Either.monad(), Either.right("hola mundo"));
  }

  @Test
  public void validationMonad() {
    verifyLaws(Validation.monad(), Validation.valid("hola mundo"));
  }
}
