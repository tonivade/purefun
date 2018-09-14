/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.monad;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.type.Try;

public class KleisliTest {

  @Test
  public void compose() {
    Function1<String, Integer> toIntF = Integer::parseInt;
    Function1<Integer, Double> halfF = i -> i / 2.;

    Kleisli<Try.µ, String, Integer> toInt = Kleisli.lift(Try.monad(), toIntF);
    Kleisli<Try.µ, Integer, Double> half = Kleisli.lift(Try.monad(), halfF);

    Higher1<Try.µ, Double> result = toInt.compose(Try.monad(), half).run("123");

    assertEquals(Try.success(61.5), result);
  }
}
