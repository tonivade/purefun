/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import static com.github.tonivade.purefun.Precondition.checkNonNull;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Higher2;
import com.github.tonivade.purefun.monad.Writer;
import com.github.tonivade.purefun.monad.WriterOf;
import com.github.tonivade.purefun.monad.Writer_;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.Monoid;

public interface WriterInstances {

  static <L> Monad<Higher1<Writer_, L>> monad(Monoid<L> monoid) {
    return WriterMonad.instance(checkNonNull(monoid));
  }
}

interface WriterMonad<L> extends Monad<Higher1<Writer_, L>> {

  static <L> WriterMonad<L> instance(Monoid<L> monoid) {
    return () -> monoid;
  }

  Monoid<L> monoid();

  @Override
  default <T> Higher2<Writer_, L, T> pure(T value) {
    return Writer.pure(monoid(), value);
  }

  @Override
  default <T, R> Higher2<Writer_, L, R> flatMap(Higher1<Higher1<Writer_, L>, T> value,
      Function1<T, ? extends Higher1<Higher1<Writer_, L>, R>> map) {
    return WriterOf.narrowK(value).flatMap(map.andThen(WriterOf::narrowK));
  }
}
