/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.monad.Reader;
import com.github.tonivade.purefun.monad.ReaderOf;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.MonadReader;

@SuppressWarnings("unchecked")
public interface ReaderInstances {

  static <R> Monad<Kind<Reader<?, ?>, R>> monad() {
    return ReaderMonad.INSTANCE;
  }

  static <R> MonadReader<Kind<Reader<?, ?>, R>, R> monadReader() {
    return ReaderMonadReader.INSTANCE;
  }
}

interface ReaderMonad<R> extends Monad<Kind<Reader<?, ?>, R>> {

  @SuppressWarnings("rawtypes")
  ReaderMonad INSTANCE = new ReaderMonad() {};

  @Override
  default <T> Reader<R, T> pure(T value) {
    return Reader.pure(value);
  }

  @Override
  default <T, V> Reader<R, V> flatMap(Kind<Kind<Reader<?, ?>, R>, ? extends T> value,
      Function1<? super T, ? extends Kind<Kind<Reader<?, ?>, R>, ? extends V>> map) {
    return ReaderOf.narrowK(value).flatMap(map.andThen(ReaderOf::narrowK));
  }
}

interface ReaderMonadReader<R> extends MonadReader<Kind<Reader<?, ?>, R>, R>, ReaderMonad<R> {

  @SuppressWarnings("rawtypes")
  ReaderMonadReader INSTANCE = new ReaderMonadReader() {};

  @Override
  default Kind<Kind<Reader<?, ?>, R>, R> ask() {
    return Reader.env();
  }
}
