/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.effect.UIOOf.toUIO;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.effect.UIO_;
import com.github.tonivade.purefun.instances.UIOInstances;

public class UIOReferenceTest extends ReferenceTest<UIO_> {

  @Override
  protected <T> Reference<UIO_, T> makeRef(T value) {
    return UIOInstances.monadDefer().ref(value);
  }

  @Override
  protected <T, R> Kind<UIO_, R> doAndThen(Kind<UIO_, T> now, Kind<UIO_, R> next) {
    return UIOInstances.monad().andThen(now, () -> next);
  }

  @Override
  protected <T> T run(Kind<UIO_, T> value) {
    return value.fix(toUIO()).unsafeRunSync();
  }
}
