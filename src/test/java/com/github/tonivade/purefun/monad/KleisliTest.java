/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.monad;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Tuple;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.instances.TryInstances;
import com.github.tonivade.purefun.type.Try;

public class KleisliTest {

  @Test
  public void compose() {
    Kleisli<Try.µ, String, Integer> toInt = Kleisli.lift(TryInstances.monad(), Integer::parseInt);
    Kleisli<Try.µ, Integer, Double> half = Kleisli.lift(TryInstances.monad(), i -> i / 2.);

    Higher1<Try.µ, Double> result = toInt.compose(half).run("123");

    assertEquals(Try.success(61.5), result);
  }

  @Test
  public void flatMap() {
    Kleisli<Try.µ, String, Integer> toInt = Kleisli.lift(TryInstances.monad(), Integer::parseInt);
    Kleisli<Try.µ, String, Double> toDouble = Kleisli.lift(TryInstances.monad(), Double::parseDouble);

    Kleisli<Try.µ, String, Tuple2<Integer, Double>> flatMap =
        toInt.flatMap(integer -> toDouble.map(double_ -> Tuple.of(integer, double_)));

    assertEquals(Try.success(Tuple.of(123, 123.)), flatMap.run("123"));
  }
}
