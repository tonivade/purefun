/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Instance;
import com.github.tonivade.purefun.control.Control;
import com.github.tonivade.purefun.typeclasses.Monad;

public interface ControlInstances {

  static Monad<Control.µ> monad() {
    return ControlMonad.instance();
  }
}

@Instance
interface ControlMonad extends Monad<Control.µ> {

  @Override
  default <T> Higher1<Control.µ, T> pure(T value) {
    return Control.pure(value).kind1();
  }

  @Override
  default <T, R> Higher1<Control.µ, R> flatMap(
      Higher1<Control.µ, T> value, Function1<T, ? extends Higher1<Control.µ, R>> map) {
    return value.fix1(Control::narrowK).flatMap(map.andThen(Control::narrowK)).kind1();
  }
}
