/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.effect.UIO_;

public class UIOResourceTest extends ResourceTest<UIO_> {

  public UIOResourceTest() {
    super(UIO_.class);
  }
}