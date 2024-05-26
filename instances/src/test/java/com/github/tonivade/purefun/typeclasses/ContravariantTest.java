/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.laws.ContravariatLaws.verifyLaws;
import static com.github.tonivade.purefun.typeclasses.Conested.conest;
import static com.github.tonivade.purefun.typeclasses.Conested.counnest;
import static com.github.tonivade.purefun.typeclasses.Nested.nest;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.instances.ConstInstances;
import com.github.tonivade.purefun.instances.Function1Instances;
import com.github.tonivade.purefun.instances.IdInstances;
import com.github.tonivade.purefun.type.Const;
import com.github.tonivade.purefun.type.Id;

public class ContravariantTest {

  @Test
  public void constInstance() {
    Contravariant<Const<String, ?>> instance = ConstInstances.contravariant();

    verifyLaws(instance, Const.<String, String>of("string"));
  }

  @Test
  public void function1Instance() {
    Contravariant<Conested<Function1<?, ?>, Double>> instance = Function1Instances.<Double>contravariant();

    Function1<Integer, Double> int2double = Integer::doubleValue;
    Function1<String, Integer> string2Int = String::length;

    Kind<Conested<Function1<?, ?>, Double>, Integer> conest = conest(int2double);
    Kind<Conested<Function1<?, ?>, Double>, String> contramap = instance.contramap(conest, string2Int);
    Function1<String, Double> result = (Function1<String, Double>) counnest(contramap);

    assertEquals(4.0, result.apply("hola"));
  }

  @Test
  public void composedCovariantContravariant() {
    Functor<Id<?>> functor = IdInstances.functor();
    Contravariant<Const<String, ?>> contravariant = ConstInstances.contravariant();
    Contravariant<Nested<Id<?>, Const<String, ?>>> instance = Contravariant.compose(functor, contravariant);

    verifyLaws(instance, nest(Id.of(Const.<String, String>of("string"))));
  }
}
