/*
 * Copyright (c) 2018-2026, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Tuple1;
import com.github.tonivade.purefun.core.Tuple1Of;
import com.github.tonivade.purefun.typeclasses.Functor;

public interface TupleInstances {

  static Functor<Tuple1<?>> functor() {
    return Tuple1Functor.INSTANCE;
  }
}

interface Tuple1Functor extends Functor<Tuple1<?>> {

  Tuple1Functor INSTANCE = new Tuple1Functor() {};

  @Override
  default <T, R> Kind<Tuple1<?>, R> map(Kind<Tuple1<?>, ? extends T> value, Function1<? super T, ? extends R> map) {
    return value.fix(Tuple1Of::toTuple1).map1(map);
  }
}
