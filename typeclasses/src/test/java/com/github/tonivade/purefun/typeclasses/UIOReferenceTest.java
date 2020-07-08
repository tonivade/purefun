/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.effect.UIOOf;
import com.github.tonivade.purefun.effect.UIO_;
import com.github.tonivade.purefun.instances.UIOInstances;

class UIOReferenceTest extends ReferenceTest<UIO_> {

  @Override
  <T> Reference<UIO_, T> makeRef(T value) {
    return UIOInstances.ref(value);
  }

  @Override
  <T, R> Kind<UIO_, R> doAndThen(Kind<UIO_, T> now, Kind<UIO_, R> next) {
    return UIOInstances.monad().andThen(now, () -> next);
  }

  @Override
  <T> T run(Kind<UIO_, T> value) {
    return UIOOf.narrowK(value).unsafeRunSync();
  }
}
