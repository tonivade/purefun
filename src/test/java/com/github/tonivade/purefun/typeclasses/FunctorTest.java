/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.Nested.nest;
import static com.github.tonivade.purefun.typeclasses.FunctorLaws.verifyLaws;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.Nested;
import com.github.tonivade.purefun.data.Sequence;
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

  @Test
  public void traverseFunctor() {
    verifyLaws(Sequence.traverse(), Sequence.listOf("hola mundo!"));
  }

  @Test
  public void composed() {
    Functor<Nested<Option.µ, Id.µ>> composed = Functor.compose(Option.functor(), Id.functor());

    assertEquals(Option.some(Id.of("HOLA!")), composed.map(nest(Option.some(Id.of("hola!"))), String::toUpperCase));
  }
}
