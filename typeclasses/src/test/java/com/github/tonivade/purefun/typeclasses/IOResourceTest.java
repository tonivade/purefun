/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.monad.IO_;

public class IOResourceTest extends ResourceTest<IO_> {

  public IOResourceTest() {
    super(IO_.class);
  }
}