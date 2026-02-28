/*
 * Copyright (c) 2018-2026, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.laws.ContravariatLaws.verifyLaws;
import static com.github.tonivade.purefun.typeclasses.Nested.nest;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.instances.ConstInstances;
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
  public void composedCovariantContravariant() {
    Functor<Id<?>> functor = IdInstances.functor();
    Contravariant<Const<String, ?>> contravariant = ConstInstances.contravariant();
    Contravariant<Nested<Id<?>, Const<String, ?>>> instance = Contravariant.compose(functor, contravariant);

    verifyLaws(instance, nest(Id.of(Const.<String, String>of("string"))));
  }
}
