/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.monad.IO_;

public class IOReferenceTest extends ReferenceTest<IO_> {

  public IOReferenceTest() {
    super(IO_.class);
  }
}
