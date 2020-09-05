/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.effect.UIOOf;
import com.github.tonivade.purefun.effect.UIO_;
import com.github.tonivade.purefun.instances.UIOInstances;

public class UIOResourceTest extends ResourceTest<UIO_> {

  @Override
  protected MonadDefer<UIO_> monadDefer() {
    return UIOInstances.monadDefer();
  }
  
  @Override
  protected <T> Resource<UIO_, T> makeResource(Kind<UIO_, T> acquire, Consumer1<T> release) {
    return UIOInstances.resource(UIOOf.narrowK(acquire), release);
  }

  @Override
  protected <T> T run(Kind<UIO_, T> result) {
    return UIOOf.narrowK(result).unsafeRunSync();
  }
}