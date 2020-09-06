/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.effect.UIOOf.toUIO;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.effect.UIO_;
import com.github.tonivade.purefun.instances.UIOInstances;

public class UIOResourceTest extends ResourceTest<UIO_> {

  @Override
  protected MonadDefer<UIO_> monadDefer() {
    return UIOInstances.monadDefer();
  }

  @Override
  protected <T> T run(Kind<UIO_, T> result) {
    return result.fix(toUIO()).unsafeRunSync();
  }
}