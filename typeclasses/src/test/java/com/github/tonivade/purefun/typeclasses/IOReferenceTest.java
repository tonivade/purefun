/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.instances.IOInstances;
import static com.github.tonivade.purefun.monad.IOOf.toIO;
import com.github.tonivade.purefun.monad.IO_;

public class IOReferenceTest extends ReferenceTest<IO_> {

  @Override
  protected <T> Reference<IO_, T> makeRef(T value) {
    return IOInstances.monadDefer().ref(value);
  }

  @Override
  protected <T, R> Kind<IO_, R> doAndThen(Kind<IO_, T> now, Kind<IO_, R> next) {
    return IOInstances.monad().andThen(now, () -> next);
  }

  @Override
  protected <T> T run(Kind<IO_, T> value) {
    return value.fix(toIO()).unsafeRunSync();
  }
}
