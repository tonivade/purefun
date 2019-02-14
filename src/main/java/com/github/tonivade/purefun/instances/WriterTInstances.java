/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import static java.util.Objects.requireNonNull;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.monad.WriterT;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.Monoid;

public interface WriterTInstances {
  static <F extends Kind, L> Monad<Higher1<Higher1<WriterT.µ, F>, L>> monad(Monoid<L> monoid, Monad<F> monadF) {
    requireNonNull(monoid);
    requireNonNull(monadF);
    return new WriterTMonad<F, L>() {
      
      @Override
      public Monoid<L> monoid() { return monoid; }
      
      @Override
      public Monad<F> monadF() { return monadF; }
    };
  }
}

interface WriterTMonad<F extends Kind, L> extends Monad<Higher1<Higher1<WriterT.µ, F>, L>> {

  Monad<F> monadF();
  Monoid<L> monoid();
  
  @Override
  default <T> WriterT<F, L, T> pure(T value) {
    return WriterT.pure(monoid(), monadF(), value);
  }

  @Override
  default <T, R> WriterT<F, L, R> flatMap(Higher1<Higher1<Higher1<WriterT.µ, F>, L>, T> value,
      Function1<T, ? extends Higher1<Higher1<Higher1<WriterT.µ, F>, L>, R>> map) {
    return WriterT.narrowK(value).flatMap(map.andThen(WriterT::narrowK));
  }
}
