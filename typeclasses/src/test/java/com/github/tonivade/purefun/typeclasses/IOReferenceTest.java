/*
 * Copyright (c) 2018-2025, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.monad.IO;

public class IOReferenceTest extends ReferenceTest<IO<?>> {

  public IOReferenceTest() {
    super(new Instance<IO<?>>() {});
  }
}
