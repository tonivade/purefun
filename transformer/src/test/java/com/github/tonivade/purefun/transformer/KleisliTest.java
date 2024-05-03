/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.transformer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.core.Tuple;
import com.github.tonivade.purefun.core.Tuple2;
import com.github.tonivade.purefun.instances.TryInstances;
import com.github.tonivade.purefun.type.Try;

public class KleisliTest {

  @Test
  public void compose() {
    Kleisli<Try<?>, String, Integer> toInt = Kleisli.lift(TryInstances.monad(), Integer::parseInt);
    Kleisli<Try<?>, Integer, Double> half = Kleisli.lift(TryInstances.monad(), i -> i / 2.);

    Kind<Try<?>, Double> result = toInt.compose(half).run("123");

    assertEquals(Try.success(61.5), result);
  }

  @Test
  public void flatMap() {
    Kleisli<Try<?>, String, Integer> toInt = Kleisli.lift(TryInstances.monad(), Integer::parseInt);
    Kleisli<Try<?>, String, Double> toDouble = Kleisli.lift(TryInstances.monad(), Double::parseDouble);

    Kleisli<Try<?>, String, Tuple2<Integer, Double>> flatMap =
        toInt.flatMap(integer -> toDouble.map(double_ -> Tuple.of(integer, double_)));

    assertEquals(Try.success(Tuple.of(123, 123.)), flatMap.run("123"));
  }
}
