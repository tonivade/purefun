/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Higher2;
import com.github.tonivade.purefun.Instance;
import com.github.tonivade.purefun.monad.Reader;
import com.github.tonivade.purefun.typeclasses.Monad;

public interface ReaderInstances {

  static <R> Monad<Higher1<Reader.µ, R>> monad() {
    return new ReaderMonad<R>() {};
  }
}

@Instance
interface ReaderMonad<R> extends Monad<Higher1<Reader.µ, R>> {

  @Override
  default <T> Higher2<Reader.µ, R, T> pure(T value) {
    return Reader.<R, T>pure(value).kind2();
  }

  @Override
  default <T, V> Higher2<Reader.µ, R, V> flatMap(Higher1<Higher1<Reader.µ, R>, T> value,
      Function1<T, ? extends Higher1<Higher1<Reader.µ, R>, V>> map) {
    return Reader.narrowK(value).flatMap(map.andThen(Reader::<R, V>narrowK)).kind2();
  }
}
