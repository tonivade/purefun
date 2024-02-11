/*
 * Copyright (c) 2018-2023, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.core.Function1Of;
import com.github.tonivade.purefun.core.Function1_;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.instances.Function1Instances;

public class ProfunctorTest {

  private final Function1<String, Integer> parseInt = Integer::parseInt;
  private final Function1<Integer, String> toString = String::valueOf;
  private final Function1<Integer, Double> toDouble = Integer::doubleValue;

  @Test
  public void dimap() {
    Profunctor<Function1_> profunctor = Function1Instances.profunctor();

    Kind<Kind<Function1_, Integer>, Double> result = profunctor.dimap(parseInt, toString, toDouble);

    assertEquals(2.0, result.fix(Function1Of::<Integer, Double>narrowK).apply(2));
  }
}
