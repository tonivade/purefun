/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.typeclasses.ApplicativeLaws.verifyLaws;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Operator5;
import com.github.tonivade.purefun.instances.OptionInstances;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Id;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.type.Try;
import com.github.tonivade.purefun.type.Validation;

public class ApplicativeTest {

  private final Operator5<Integer> sum = (a, b, c, d, e) -> a + b + c + d + e;

  @Test
  public void idApplicative() {
    verifyLaws(Id.applicative());
  }

  @Test
  public void optionApplicative() {
    verifyLaws(OptionInstances.applicative());
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

  @Test
  public void composedAplicative() {
    verifyLaws(Applicative.compose(OptionInstances.applicative(), Id.applicative()));
  }

  @Test
  public void map5Some() {
    Higher1<Option.µ, Integer> map5 =
        OptionInstances.applicative().map5(Option.some(1), Option.some(2), Option.some(3), Option.some(4), Option.some(5), sum);

    assertEquals(Option.some(15), map5);
  }

  @Test
  public void map5None() {
    Higher1<Option.µ, Integer> map5 =
        OptionInstances.applicative().map5(Option.some(1), Option.some(2), Option.some(3), Option.some(4), Option.none(), sum);

    assertEquals(Option.none(), map5);
  }
}
