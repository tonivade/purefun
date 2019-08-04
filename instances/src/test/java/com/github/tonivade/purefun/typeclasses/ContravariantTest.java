/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.instances.ConstInstances;
import com.github.tonivade.purefun.laws.ContravariatLaws;
import com.github.tonivade.purefun.type.Const;

public class ContravariantTest {

  @Test
  public void constInstance() {
    Contravariant<Higher1<Const.µ, String>> instance = ConstInstances.contravariant();

    ContravariatLaws.verifyLaws(instance, Const.of("string"));
  }
}
