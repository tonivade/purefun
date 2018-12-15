/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.typeclasses.ApplicativeLaws.verifyLaws;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.type.Try;
import com.github.tonivade.purefun.type.Validation;

public class ApplicativeTest {

  @Test
  public void optionApplicative() {
    verifyLaws(Option.applicative());
  }

  @Test
  public void tryApplicative() {
    verifyLaws(Try.applicative());
  }

  @Test
  public void eitherApplicative() {
    verifyLaws(Either.applicative());
  }

  @Test
  public void validationApplicative() {
    verifyLaws(Validation.applicative());
  }
}
