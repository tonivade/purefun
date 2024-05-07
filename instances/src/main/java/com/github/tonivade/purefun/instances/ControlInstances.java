/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.control.Control;
import com.github.tonivade.purefun.control.ControlOf;
import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.typeclasses.Monad;

public interface ControlInstances {

  static Monad<Control<?>> monad() {
    return ControlMonad.INSTANCE;
  }
}

interface ControlMonad extends Monad<Control<?>> {

  ControlMonad INSTANCE = new ControlMonad() {};

  @Override
  default <T> Control<T> pure(T value) {
    return Control.pure(value);
  }

  @Override
  default <T, R> Control<R> flatMap(
      Kind<Control<?>, ? extends T> value, Function1<? super T, ? extends Kind<Control<?>, ? extends R>> map) {
    return value.fix(ControlOf::toControl).flatMap(map.andThen(ControlOf::toControl));
  }
}
