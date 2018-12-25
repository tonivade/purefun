/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.typeclasses.FunctorLaws.verifyLaws;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Id;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.type.Try;
import com.github.tonivade.purefun.type.Validation;

public class FunctorTest {

  @Test
  public void idFunctor() {
    verifyLaws(Id.functor(), Id.of("hola mundo!"));
  }

  @Test
  public void optionFunctor() {
    verifyLaws(Option.functor(), Option.some("hola mundo!"));
  }

  @Test
  public void tryFunctor() {
    verifyLaws(Try.functor(), Try.success("hola mundo!"));
  }

  @Test
  public void eitherFunctor() {
    verifyLaws(Either.functor(), Either.right("hola mundo!"));
  }

  @Test
  public void validationFunctor() {
    verifyLaws(Validation.functor(), Validation.valid("hola mundo!"));
  }
}
