/*
 * Copyright (c) 2018-2023, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import static com.github.tonivade.purefun.control.ControlOf.toControl;

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
      Kind<Control_, ? extends T> value, Function1<? super T, ? extends Kind<Control_, ? extends R>> map) {
    return value.fix(toControl()).flatMap(map.andThen(ControlOf::narrowK));
  }
}
