/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import static com.github.tonivade.purefun.Precondition.checkNonNull;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Higher3;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Operator1;
import com.github.tonivade.purefun.Tuple;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.transformer.WriterT;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.MonadError;
import com.github.tonivade.purefun.typeclasses.MonadWriter;
import com.github.tonivade.purefun.typeclasses.Monoid;

public interface WriterTInstances {

  static <F extends Kind, L> Monad<Higher1<Higher1<WriterT.µ, F>, L>> monad(Monoid<L> monoid, Monad<F> monadF) {
    return WriterTMonad.instance(checkNonNull(monoid), checkNonNull(monadF));
  }

  static <F extends Kind, L> MonadWriter<Higher1<Higher1<WriterT.µ, F>, L>, L> monadWriter(Monoid<L> monoid, Monad<F> monadF) {
    return WriterTMonadWriter.instance(checkNonNull(monoid), checkNonNull(monadF));
  }

  static <F extends Kind, L, E> MonadError<Higher1<Higher1<WriterT.µ, F>, L>, E> monadError(
      Monoid<L> monoid, MonadError<F, E> monadErrorF) {
    return WriterTMonadError.instance(checkNonNull(monoid), checkNonNull(monadErrorF));
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

interface WriterTMonadWriter<F extends Kind, L>
    extends MonadWriter<Higher1<Higher1<WriterT.µ, F>, L>, L>, WriterTMonad<F, L> {

  static <F extends Kind, L> MonadWriter<Higher1<Higher1<WriterT.µ, F>, L>, L> instance(Monoid<L> monoid, Monad<F> monadF) {
    return new WriterTMonadWriter<F, L>() {

      @Override
      public Monoid<L> monoid() { return monoid; }

      @Override
      public Monad<F> monadF() { return monadF; }
    };
  }

  @Override
  default <A> Higher3<WriterT.µ, F, L, A> writer(Tuple2<L, A> value) {
    return WriterT.lift(monoid(), monadF(), value).kind3();
  }

  @Override
  default <A> Higher3<WriterT.µ, F, L, Tuple2<L, A>> listen(Higher1<Higher1<Higher1<WriterT.µ, F>, L>, A> value) {
    return value.fix1(WriterT::narrowK).listen().kind3();
  }

  @Override
  default <A> Higher3<WriterT.µ, F, L, A> pass(
      Higher1<Higher1<Higher1<WriterT.µ, F>, L>, Tuple2<Operator1<L>, A>> value) {
    WriterT<F, L, Tuple2<Operator1<L>, A>> writerT = value.fix1(WriterT::narrowK);
    return writerT.listen().flatMap((Tuple2<L, Tuple2<Operator1<L>, A>> tuple) -> {
        Operator1<L> operator = tuple.get2().get1();
        A value2 = tuple.get2().get2();
      return writer(Tuple.of(operator.apply(tuple.get1()), value2)).fix1(WriterT::narrowK);
      }).kind3();
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
