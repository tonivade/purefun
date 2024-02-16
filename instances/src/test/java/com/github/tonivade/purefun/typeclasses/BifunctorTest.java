/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.laws.BifunctorLaws.verifyLaws;
import static org.junit.jupiter.api.Assertions.assertAll;
import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.core.Tuple;
import com.github.tonivade.purefun.instances.EitherInstances;
import com.github.tonivade.purefun.instances.TupleInstances;
import com.github.tonivade.purefun.instances.ValidationInstances;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Validation;

public class BifunctorTest {

  @Test
  public void eitherBifunctor() {
    assertAll(
        () -> verifyLaws(EitherInstances.bifunctor(), Either.<String, String>right("hola mundo")),
        () -> verifyLaws(EitherInstances.bifunctor(), Either.<String, String>left("hola mundo")));
  }

  @Test
  public void validationBifunctor() {
    assertAll(
        () -> verifyLaws(ValidationInstances.bifunctor(), Validation.<String, String>valid("hola mundo")),
        () -> verifyLaws(ValidationInstances.bifunctor(), Validation.<String, String>invalid("hola mundo")));
  }

  @Test
  public void tupleBifunctor() {
    verifyLaws(TupleInstances.bifunctor(), Tuple.of("hola mundo", "adios"));
  }
}
