/*
 * Copyright (c) 2018-2022, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.control;

import com.github.tonivade.purefun.Function1;

public interface Handler<R, E> extends Marker.Cont<R>, Effect<E> {

  default <T> Control<T> use(Function1<Function1<T, Control<R>>, Control<R>> body) {
    return Control.use(this, body);
  }

  default Control<R> apply(Function1<E, Control<R>> program) {
    if (this instanceof StateMarker) {
      return Control.delimitState((Marker.State<?>) this,
          Control.delimitCont(this, h -> program.apply(effect())));
    }
    return Control.delimitCont(this, h -> program.apply(effect()));
  }
}
