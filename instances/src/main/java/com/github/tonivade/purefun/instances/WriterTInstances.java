/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import static com.github.tonivade.purefun.Precondition.checkNonNull;
import static com.github.tonivade.purefun.transformer.WriterTOf.toWriterT;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Operator1;
import com.github.tonivade.purefun.Tuple;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.Witness;
import com.github.tonivade.purefun.transformer.WriterT;
import com.github.tonivade.purefun.transformer.WriterTOf;
import com.github.tonivade.purefun.transformer.WriterT_;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.MonadError;
import com.github.tonivade.purefun.typeclasses.MonadWriter;
import com.github.tonivade.purefun.typeclasses.Monoid;

public interface WriterTInstances {

  static <F extends Witness, L> Monad<Kind<Kind<WriterT_, F>, L>> monad(Monoid<L> monoid, Monad<F> monadF) {
    return WriterTMonad.instance(checkNonNull(monoid), checkNonNull(monadF));
  }

  static <F extends Witness, L> MonadWriter<Kind<Kind<WriterT_, F>, L>, L> monadWriter(Monoid<L> monoid, Monad<F> monadF) {
    return WriterTMonadWriter.instance(checkNonNull(monoid), checkNonNull(monadF));
  }

  static <F extends Witness, L, E> MonadError<Kind<Kind<WriterT_, F>, L>, E> monadError(
      Monoid<L> monoid, MonadError<F, E> monadErrorF) {
    return WriterTMonadError.instance(checkNonNull(monoid), checkNonNull(monadErrorF));
  }
}

interface WriterTMonad<F extends Witness, L> extends Monad<Kind<Kind<WriterT_, F>, L>> {

  static <F extends Witness, L> Monad<Kind<Kind<WriterT_, F>, L>> instance(Monoid<L> monoid, Monad<F> monadF) {
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
  default <T> WriterT<F, L, T> pure(T value) {
    return WriterT.pure(monoid(), monadF(), value);
  }

  @Override
  default <T, R> WriterT<F, L, R> flatMap(Kind<Kind<Kind<WriterT_, F>, L>, ? extends T> value,
      Function1<? super T, ? extends Kind<Kind<Kind<WriterT_, F>, L>, ? extends R>> map) {
    return WriterTOf.narrowK(value).flatMap(map.andThen(WriterTOf::narrowK));
  }
}

interface WriterTMonadWriter<F extends Witness, L>
    extends MonadWriter<Kind<Kind<WriterT_, F>, L>, L>, WriterTMonad<F, L> {

  static <F extends Witness, L> MonadWriter<Kind<Kind<WriterT_, F>, L>, L> instance(Monoid<L> monoid, Monad<F> monadF) {
    return new WriterTMonadWriter<F, L>() {

      @Override
      public Monoid<L> monoid() { return monoid; }

      @Override
      public Monad<F> monadF() { return monadF; }
    };
  }

  @Override
  default <A> WriterT<F, L, A> writer(Tuple2<L, A> value) {
    return WriterT.lift(monoid(), monadF(), value);
  }

  @Override
  default <A> WriterT<F, L, Tuple2<L, A>> listen(Kind<Kind<Kind<WriterT_, F>, L>, A> value) {
    return value.fix(WriterTOf::narrowK).listen();
  }

  @Override
  default <A> WriterT<F, L, A> pass(
      Kind<Kind<Kind<WriterT_, F>, L>, Tuple2<Operator1<L>, A>> value) {
    WriterT<F, L, Tuple2<Operator1<L>, A>> writerT = value.fix(WriterTOf::narrowK);
    return writerT.listen().flatMap((Tuple2<L, Tuple2<Operator1<L>, A>> tuple) -> {
        Operator1<L> operator = tuple.get2().get1();
        A value2 = tuple.get2().get2();
      return writer(Tuple.of(operator.apply(tuple.get1()), value2)).fix(WriterTOf::narrowK);
      });
  }
}

interface WriterTMonadError<F extends Witness, L, E>
    extends MonadError<Kind<Kind<WriterT_, F>, L>, E>, WriterTMonad<F, L> {

  static <F extends Witness, L, E> MonadError<Kind<Kind<WriterT_, F>, L>, E> instance(
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
  default <A> WriterT<F, L, A> raiseError(E error) {
    return WriterT.writer(monoid(), monadF(),
        monadF().map(monadF().<A>raiseError(error), a -> Tuple.of(monoid().zero(), a)));
  }

  @Override
  default <A> WriterT<F, L, A> handleErrorWith(
      Kind<Kind<Kind<WriterT_, F>, L>, A> value,
      Function1<? super E, ? extends Kind<Kind<Kind<WriterT_, F>, L>, ? extends A>> handler) {
    return WriterT.writer(monoid(), monadF(),
        monadF().handleErrorWith(value.fix(toWriterT()).value(),
            error -> handler.apply(error).fix(WriterTOf::<F, L, A>narrowK).value()));
  }
}
