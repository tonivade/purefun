/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.laws.BifunctorLaws.verifyLaws;
import static org.junit.jupiter.api.Assertions.assertAll;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.Tuple;
import com.github.tonivade.purefun.instances.EitherInstances;
import com.github.tonivade.purefun.instances.TupleInstances;
import com.github.tonivade.purefun.instances.ValidationInstances;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Validation;

public class BifunctorTest {

  @Test
  public void eitherBifunctor() {
    assertAll(
        () -> verifyLaws(EitherInstances.bifunctor(), Either.right("hola mundo")),
        () -> verifyLaws(EitherInstances.bifunctor(), Either.left("hola mundo")));
  }

  @Test
  public void validationBifunctor() {
    assertAll(
        () -> verifyLaws(ValidationInstances.bifunctor(), Validation.valid("hola mundo")),
        () -> verifyLaws(ValidationInstances.bifunctor(), Validation.invalid("hola mundo")));
  }

  @Test
  public void tupleBifunctor() {
    verifyLaws(TupleInstances.bifunctor(), Tuple.of("hola mundo", "adios"));
  }
}
