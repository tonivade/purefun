/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Higher2;
import com.github.tonivade.purefun.monad.Reader;
import com.github.tonivade.purefun.monad.ReaderOf;
import com.github.tonivade.purefun.monad.Reader_;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.MonadReader;

@SuppressWarnings("unchecked")
public interface ReaderInstances {

  static <R> Monad<Higher1<Reader_, R>> monad() {
    return ReaderMonad.INSTANCE;
  }

  static <R> MonadReader<Higher1<Reader_, R>, R> monadReader() {
    return ReaderMonadReader.INSTANCE;
  }
}

interface ReaderMonad<R> extends Monad<Higher1<Reader_, R>> {

  @SuppressWarnings("rawtypes")
  ReaderMonad INSTANCE = new ReaderMonad() {};

  @Override
  default <T> Higher2<Reader_, R, T> pure(T value) {
    return Reader.<R, T>pure(value);
  }

  @Override
  default <T, V> Higher2<Reader_, R, V> flatMap(Higher1<Higher1<Reader_, R>, T> value,
      Function1<T, ? extends Higher1<Higher1<Reader_, R>, V>> map) {
    return ReaderOf.narrowK(value).flatMap(map.andThen(ReaderOf::narrowK));
  }
}

interface ReaderMonadReader<R> extends MonadReader<Higher1<Reader_, R>, R>, ReaderMonad<R> {

  @SuppressWarnings("rawtypes")
  ReaderMonadReader INSTANCE = new ReaderMonadReader() {};

  @Override
  default Higher1<Higher1<Reader_, R>, R> ask() {
    return Reader.<R>env();
  }
}
