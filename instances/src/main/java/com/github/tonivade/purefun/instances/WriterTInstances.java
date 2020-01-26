/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import static java.util.Objects.requireNonNull;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Higher3;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Tuple;
import com.github.tonivade.purefun.transformer.WriterT;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.MonadError;
import com.github.tonivade.purefun.typeclasses.Monoid;

public interface WriterTInstances {

  static <F extends Kind, L> Monad<Higher1<Higher1<WriterT.µ, F>, L>> monad(Monoid<L> monoid, Monad<F> monadF) {
    return WriterTMonad.instance(requireNonNull(monoid), requireNonNull(monadF));
  }

  static <F extends Kind, L, E> MonadError<Higher1<Higher1<WriterT.µ, F>, L>, E> monadError(
      Monoid<L> monoid, MonadError<F, E> monadErrorF) {
    return WriterTMonadError.instance(requireNonNull(monoid), requireNonNull(monadErrorF));
  }
}

interface WriterTMonad<F extends Kind, L> extends Monad<Higher1<Higher1<WriterT.µ, F>, L>> {

  static <F extends Kind, L> Monad<Higher1<Higher1<WriterT.µ, F>, L>> instance(Monoid<L> monoid, Monad<F> monadF) {
    return new WriterTMonad<F, L>() {

      @Override
      public Monoid<L> monoid() { return monoid; }

      @Override
      public Monad<F> monadF() { return monadF; }
    };
  }

  Monad<F> monadF();
  Monoid<L> monoid();

  @Override
  default <T> Higher3<WriterT.µ, F, L, T> pure(T value) {
    return WriterT.pure(monoid(), monadF(), value).kind3();
  }

  @Override
  default <T, R> Higher3<WriterT.µ, F, L, R> flatMap(Higher1<Higher1<Higher1<WriterT.µ, F>, L>, T> value,
      Function1<T, ? extends Higher1<Higher1<Higher1<WriterT.µ, F>, L>, R>> map) {
    return WriterT.narrowK(value).flatMap(map.andThen(WriterT::narrowK)).kind3();
  }
}

interface WriterTMonadError<F extends Kind, L, E>
    extends MonadError<Higher1<Higher1<WriterT.µ, F>, L>, E>, WriterTMonad<F, L> {

  static <F extends Kind, L, E> MonadError<Higher1<Higher1<WriterT.µ, F>, L>, E> instance(
      Monoid<L> monoid, MonadError<F, E> monadErrorF) {
    return new WriterTMonadError<F, L, E>() {

      @Override
      public Monoid<L> monoid() { return monoid; }

      @Override
      public MonadError<F, E> monadF() { return monadErrorF; }
    };
  }

  @Override
  MonadError<F, E> monadF();

  @Override
  default <A> Higher3<WriterT.µ, F, L, A> raiseError(E error) {
    return WriterT.writer(monoid(), monadF(),
        monadF().map(monadF().<A>raiseError(error), a -> Tuple.of(monoid().zero(), a))).kind3();
  }

  @Override
  default <A> Higher3<WriterT.µ, F, L, A> handleErrorWith(
      Higher1<Higher1<Higher1<WriterT.µ, F>, L>, A> value,
      Function1<E, ? extends Higher1<Higher1<Higher1<WriterT.µ, F>, L>, A>> handler) {
    return WriterT.writer(monoid(), monadF(),
        monadF().handleErrorWith(value.fix1(WriterT::narrowK).value(),
            error -> handler.apply(error).fix1(WriterT::narrowK).value())).kind3();
  }
}
