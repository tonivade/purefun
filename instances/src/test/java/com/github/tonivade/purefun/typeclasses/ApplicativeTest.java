/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Operator5;
import com.github.tonivade.purefun.instances.ConstInstances;
import com.github.tonivade.purefun.instances.EitherInstances;
import com.github.tonivade.purefun.instances.IdInstances;
import com.github.tonivade.purefun.instances.OptionInstances;
import com.github.tonivade.purefun.instances.SequenceInstances;
import com.github.tonivade.purefun.instances.TryInstances;
import com.github.tonivade.purefun.instances.ValidationInstances;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.type.Option_;
import org.junit.jupiter.api.Test;

import static com.github.tonivade.purefun.laws.ApplicativeLaws.verifyLaws;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ApplicativeTest {

  private final Operator5<Integer> sum = (a, b, c, d, e) -> a + b + c + d + e;

  @Test
  public void laws() {
    assertAll(
        () -> verifyLaws(IdInstances.applicative()),
        () -> verifyLaws(OptionInstances.applicative()),
        () -> verifyLaws(TryInstances.applicative()),
        () -> verifyLaws(EitherInstances.applicative()),
        () -> verifyLaws(ConstInstances.applicative(Monoid.integer())),
        () -> verifyLaws(ValidationInstances.applicative(SequenceInstances.semigroup())),
        () -> verifyLaws(Applicative.compose(OptionInstances.applicative(), IdInstances.applicative()))
    );
  }

  @Test
  public void map5Some() {
    Higher1<Option_, Integer> map5 =
        OptionInstances.applicative().map5(
            Option.some(1),
            Option.some(2),
            Option.some(3),
            Option.some(4),
            Option.some(5), sum);

    assertEquals(Option.some(15), map5);
  }

  @Test
  public void map5None() {
    Higher1<Option_, Integer> map5 =
        OptionInstances.applicative().map5(
            Option.some(1),
            Option.some(2),
            Option.some(3),
            Option.some(4),
            Option.<Integer>none(), sum);

    assertEquals(Option.none(), map5);
  }
}
