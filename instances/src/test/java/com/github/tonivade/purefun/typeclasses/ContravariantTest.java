/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.Conested.*;
import static com.github.tonivade.purefun.Nested.nest;
import static com.github.tonivade.purefun.laws.ContravariatLaws.verifyLaws;
import static org.junit.jupiter.api.Assertions.*;

import com.github.tonivade.purefun.*;
import com.github.tonivade.purefun.instances.Function1Instances;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.instances.ConstInstances;
import com.github.tonivade.purefun.instances.IdInstances;
import com.github.tonivade.purefun.laws.ContravariatLaws;
import com.github.tonivade.purefun.type.Const;
import com.github.tonivade.purefun.type.Id;

public class ContravariantTest {

  @Test
  public void constInstance() {
    Contravariant<Higher1<Const.µ, String>> instance = ConstInstances.contravariant();

    verifyLaws(instance, Const.<String, String>of("string").kind2());
  }

  @Test
  public void function1Instance() {
    Contravariant<Conested<Function1.µ, Double>> instance = Function1Instances.<Double>contravariant();

    Function1<Integer, Double> int2double = Integer::doubleValue;
    Function1<String, Integer> string2Int = String::length;

    Higher1<Conested<Function1.µ, Double>, Integer> conest = conest(int2double.kind2());
    Higher1<Conested<Function1.µ, Double>, String> contramap = instance.contramap(conest, string2Int);
    Function1<String, Double> result = counnest(contramap).fix1(Function1::<String, Double>narrowK);

    assertEquals(4.0, result.apply("hola"));
  }

  @Test
  public void composedCovariantContravariant() {
    Functor<Id.µ> functor = IdInstances.functor();
    Contravariant<Higher1<Const.µ, String>> contravariant = ConstInstances.contravariant();
    Contravariant<Nested<Id.µ, Higher1<Const.µ, String>>> instance = Contravariant.compose(functor, contravariant);

    verifyLaws(instance, nest(Id.of(Const.<String, String>of("string").kind1()).kind1()));
  }
}
