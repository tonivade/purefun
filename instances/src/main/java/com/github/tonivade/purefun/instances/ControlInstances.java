/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.control.Control;
import com.github.tonivade.purefun.control.ControlOf;
import com.github.tonivade.purefun.control.Control_;
import com.github.tonivade.purefun.typeclasses.Monad;

public interface ControlInstances {

  static Monad<Control_> monad() {
    return ControlMonad.INSTANCE;
  }
}

interface ControlMonad extends Monad<Control_> {

  ControlMonad INSTANCE = new ControlMonad() {};

  @Override
  default <T> Control<T> pure(T value) {
    return Control.pure(value);
  }

  @Override
  default <T, R> Control<R> flatMap(
      Kind<Control_, T> value, Function1<T, ? extends Kind<Control_, R>> map) {
    return value.fix(ControlOf::narrowK).flatMap(map.andThen(ControlOf::narrowK));
  }
}
