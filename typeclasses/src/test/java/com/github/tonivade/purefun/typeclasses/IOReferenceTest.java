/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.instances.IOInstances;
import com.github.tonivade.purefun.monad.IOOf;
import com.github.tonivade.purefun.monad.IO_;

class IOReferenceTest extends ReferenceTest<IO_> {

  @Override
  <T> Reference<IO_, T> makeRef(T value) {
    return IOInstances.ref(value);
  }

  @Override
  <T, R> Kind<IO_, R> doAndThen(Kind<IO_, T> now, Kind<IO_, R> next) {
    return IOInstances.monad().andThen(now, () -> next);
  }

  @Override
  <T> T run(Kind<IO_, T> value) {
    return IOOf.narrowK(value).unsafeRunSync();
  }
}
