/*
 * Copyright (c) 2018-2023, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import static com.github.tonivade.purefun.Precondition.checkNonNull;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.monad.Writer;
import com.github.tonivade.purefun.monad.WriterOf;
import com.github.tonivade.purefun.monad.Writer_;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.Monoid;

public interface WriterInstances {

  static <L> Monad<Kind<Writer_, L>> monad(Monoid<L> monoid) {
    return WriterMonad.instance(checkNonNull(monoid));
  }
}

interface WriterMonad<L> extends Monad<Kind<Writer_, L>> {

  static <L> WriterMonad<L> instance(Monoid<L> monoid) {
    return () -> monoid;
  }

  Monoid<L> monoid();

  @Override
  default <T> Writer<L, T> pure(T value) {
    return Writer.pure(monoid(), value);
  }

  @Override
  default <T, R> Writer<L, R> flatMap(Kind<Kind<Writer_, L>, ? extends T> value,
      Function1<? super T, ? extends Kind<Kind<Writer_, L>, ? extends R>> map) {
    return WriterOf.narrowK(value).flatMap(map.andThen(WriterOf::narrowK));
  }
}
