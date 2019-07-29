/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import static java.util.Objects.requireNonNull;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Instance;
import com.github.tonivade.purefun.monad.Writer;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.Monoid;

public interface WriterInstances {

  static <L> Monad<Higher1<Writer.µ, L>> monad(Monoid<L> monoid) {
    requireNonNull(monoid);
    return new WriterMonad<L>() {

      @Override
      public Monoid<L> monoid() { return monoid; }
    };
  }
}

@Instance
interface WriterMonad<L> extends Monad<Higher1<Writer.µ, L>> {

  Monoid<L> monoid();

  @Override
  default <T> Writer<L, T> pure(T value) {
    return Writer.pure(monoid(), value);
  }

  @Override
  default <T, R> Writer<L, R> flatMap(Higher1<Higher1<Writer.µ, L>, T> value,
      Function1<T, ? extends Higher1<Higher1<Writer.µ, L>, R>> map) {
    return Writer.narrowK(value).flatMap(map.andThen(Writer::narrowK));
  }
}
