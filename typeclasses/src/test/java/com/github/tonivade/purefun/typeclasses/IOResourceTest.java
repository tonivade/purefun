/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.instances.IOInstances;
import com.github.tonivade.purefun.monad.IOOf;
import com.github.tonivade.purefun.monad.IO_;

public class IOResourceTest<F> extends ResourceTest<IO_> {

  @Override
  protected MonadDefer<IO_> monadDefer() {
    return IOInstances.monadDefer();
  }
  
  @Override
  protected <T> Resource<IO_, T> makeResource(Kind<IO_, T> acquire, Consumer1<T> release) {
    return IOInstances.resource(IOOf.narrowK(acquire), release);
  }

  @Override
  protected <T> T run(Kind<IO_, T> result) {
    return IOOf.narrowK(result).unsafeRunSync();
  }
}