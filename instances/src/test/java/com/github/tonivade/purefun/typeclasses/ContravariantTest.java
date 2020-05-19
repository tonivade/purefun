/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.laws.ContravariatLaws.verifyLaws;
import static com.github.tonivade.purefun.typeclasses.Conested.conest;
import static com.github.tonivade.purefun.typeclasses.Conested.counnest;
import static com.github.tonivade.purefun.typeclasses.Nested.nest;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Function1Of;
import com.github.tonivade.purefun.Function1_;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.instances.ConstInstances;
import com.github.tonivade.purefun.instances.Function1Instances;
import com.github.tonivade.purefun.instances.IdInstances;
import com.github.tonivade.purefun.type.Const;
import com.github.tonivade.purefun.type.Const_;
import com.github.tonivade.purefun.type.Id;
import com.github.tonivade.purefun.type.Id_;

public class ContravariantTest {

  @Test
  public void constInstance() {
    Contravariant<Higher1<Const_, String>> instance = ConstInstances.contravariant();

    verifyLaws(instance, Const.<String, String>of("string"));
  }

  @Test
  public void function1Instance() {
    Contravariant<Conested<Function1_, Double>> instance = Function1Instances.<Double>contravariant();

    Function1<Integer, Double> int2double = Integer::doubleValue;
    Function1<String, Integer> string2Int = String::length;

    Higher1<Conested<Function1_, Double>, Integer> conest = conest(int2double);
    Higher1<Conested<Function1_, Double>, String> contramap = instance.contramap(conest, string2Int);
    Function1<String, Double> result = counnest(contramap).fix1(Function1Of::<String, Double>narrowK);

    assertEquals(4.0, result.apply("hola"));
  }

  @Test
  public void composedCovariantContravariant() {
    Functor<Id_> functor = IdInstances.functor();
    Contravariant<Higher1<Const_, String>> contravariant = ConstInstances.contravariant();
    Contravariant<Nested<Id_, Higher1<Const_, String>>> instance = Contravariant.compose(functor, contravariant);

    verifyLaws(instance, nest(Id.of(Const.<String, String>of("string"))));
  }
}
