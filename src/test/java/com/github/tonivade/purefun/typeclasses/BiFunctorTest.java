/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.typeclasses.BiFunctorLaws.verifyLaws;
import static org.junit.jupiter.api.Assertions.assertAll;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Validation;

public class BiFunctorTest {

  @Test
  public void eitherBiFunctor() {
    assertAll(
        () -> verifyLaws(Either.bifunctor(), Either.right("hola mundo")),
        () -> verifyLaws(Either.bifunctor(), Either.left("hola mundo")));
  }

  @Test
  public void validationBiFunctor() {
    assertAll(
        () -> verifyLaws(Validation.bifunctor(), Validation.valid("hola mundo")),
        () -> verifyLaws(Validation.bifunctor(), Validation.invalid("hola mundo")));
  }
}
