/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.monad.IOOf.toIO;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.instances.IOInstances;
import com.github.tonivade.purefun.monad.IO_;

public class IOResourceTest extends ResourceTest<IO_> {

  @Override
  protected MonadDefer<IO_> monadDefer() {
    return IOInstances.monadDefer();
  }

  @Override
  protected <T> T run(Kind<IO_, T> result) {
    return result.fix(toIO()).unsafeRunSync();
  }
}