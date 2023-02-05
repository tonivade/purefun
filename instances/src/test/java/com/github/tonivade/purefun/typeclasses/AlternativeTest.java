/*
 * Copyright (c) 2018-2023, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.data.Sequence.listOf;
import static com.github.tonivade.purefun.laws.AlternativeLaws.verifyLaws;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.data.Sequence_;
import com.github.tonivade.purefun.instances.OptionInstances;
import com.github.tonivade.purefun.instances.SequenceInstances;

public class AlternativeTest {

  private final Alternative<Sequence_> instance = SequenceInstances.alternative();

  private final Function1<Integer, Integer> twoTimes = a -> a * 2;
  private final Function1<Integer, Integer> plusFive = a -> a + 5;

  @Test
  public void combineAndAp() {
    Kind<Sequence_, Integer> seven = instance.pure(7);
    Kind<Sequence_, Integer> eight = instance.pure(8);

    Kind<Sequence_, Integer> result =
        instance.ap(instance.combineK(seven, eight),
                    instance.combineK(instance.pure(twoTimes), instance.pure(plusFive)));

    assertEquals(listOf(14, 16, 12, 13), result);
  }

  @Test
  public void sequence() {
    assertAll(() -> verifyLaws(SequenceInstances.alternative()));
  }

  @Test
  public void option() {
    assertAll(() -> verifyLaws(OptionInstances.alternative()));
  }

  @Test
  public void composed() {
    assertAll(() -> verifyLaws(Alternative.compose(OptionInstances.alternative(), SequenceInstances.alternative())));
  }
}
