/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.laws.ComonadLaws.verifyLaws;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.instances.IdInstances;
import com.github.tonivade.purefun.type.Id;

public class ComonadTest {

  @Test
  public void idTest() {
    verifyLaws(IdInstances.comonad(), Id.of("hola mundo").kind1());
  }
}
