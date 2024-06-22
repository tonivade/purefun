/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import static com.github.tonivade.purefun.core.Precondition.checkNonNull;

import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.monad.Writer;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.Monoid;

public interface WriterInstances {

  static <L> Monad<Writer<L, ?>> monad(Monoid<L> monoid) {
    return WriterMonad.instance(checkNonNull(monoid));
  }
}

interface WriterMonad<L> extends Monad<Writer<L, ?>> {

  static <L> WriterMonad<L> instance(Monoid<L> monoid) {
    return () -> monoid;
  }

  Monoid<L> monoid();

  @Override
  default <T> Writer<L, T> pure(T value) {
    return Writer.pure(monoid(), value);
  }

  @Override
  default <T, R> Writer<L, R> flatMap(Kind<Writer<L, ?>, ? extends T> value,
      Function1<? super T, ? extends Kind<Writer<L, ?>, ? extends R>> map) {
    return value.<Writer<L, T>>fix().flatMap(map);
  }
}
