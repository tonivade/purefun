/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.Nested.nest;
import static com.github.tonivade.purefun.laws.ContravariatLaws.verifyLaws;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Nested;
import com.github.tonivade.purefun.Nothing;
import com.github.tonivade.purefun.instances.ConstInstances;
import com.github.tonivade.purefun.instances.IdInstances;
import com.github.tonivade.purefun.laws.ContravariatLaws;
import com.github.tonivade.purefun.type.Const;
import com.github.tonivade.purefun.type.Id;

public class ContravariantTest {

  @Test
  public void constInstance() {
    Contravariant<Higher1<Const.µ, String>> instance = ConstInstances.contravariant();

    verifyLaws(instance, Const.of("string"));
  }

  @Test
  public void composedCovariantContravariant() {
    Functor<Id.µ> functor = IdInstances.functor();
    Contravariant<Higher1<Const.µ, String>> contravariant = ConstInstances.contravariant();
    Contravariant<Nested<Id.µ, Higher1<Const.µ, String>>> instance = 
        Contravariant.compose(functor, contravariant);

    verifyLaws(instance, nest(Id.of(Const.of("string"))));
  }
}
