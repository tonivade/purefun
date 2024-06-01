/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import static com.github.tonivade.purefun.core.Precondition.checkNonNull;
import com.github.tonivade.purefun.Kind;

import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Operator1;
import com.github.tonivade.purefun.core.Tuple;
import com.github.tonivade.purefun.core.Tuple2;
import com.github.tonivade.purefun.transformer.WriterT;
import com.github.tonivade.purefun.transformer.WriterTOf;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.MonadError;
import com.github.tonivade.purefun.typeclasses.MonadWriter;
import com.github.tonivade.purefun.typeclasses.Monoid;

public interface WriterTInstances {

  static <F extends Kind<F, ?>, L> Monad<WriterT<F, L, ?>> monad(Monoid<L> monoid, Monad<F> monadF) {
    return WriterTMonad.instance(checkNonNull(monoid), checkNonNull(monadF));
  }

  static <F extends Kind<F, ?>, L> MonadWriter<WriterT<F, L, ?>, L> monadWriter(Monoid<L> monoid, Monad<F> monadF) {
    return WriterTMonadWriter.instance(checkNonNull(monoid), checkNonNull(monadF));
  }

  static <F extends Kind<F, ?>, L, E> MonadError<WriterT<F, L, ?>, E> monadError(
      Monoid<L> monoid, MonadError<F, E> monadErrorF) {
    return WriterTMonadError.instance(checkNonNull(monoid), checkNonNull(monadErrorF));
  }
}

interface WriterTMonad<F extends Kind<F, ?>, L> extends Monad<WriterT<F, L, ?>> {

  static <F extends Kind<F, ?>, L> Monad<WriterT<F, L, ?>> instance(Monoid<L> monoid, Monad<F> monadF) {
    return new WriterTMonad<>() {

      @Override
      public Monoid<L> monoid() {
        return monoid;
      }

      @Override
      public Monad<F> monadF() {
        return monadF;
      }
    };
  }

  Monad<F> monadF();
  Monoid<L> monoid();

  @Override
  default <T> WriterT<F, L, T> pure(T value) {
    return WriterT.pure(monoid(), monadF(), value);
  }

  @Override
  default <T, R> WriterT<F, L, R> flatMap(Kind<WriterT<F, L, ?>, ? extends T> value,
      Function1<? super T, ? extends Kind<WriterT<F, L, ?>, ? extends R>> map) {
    return WriterTOf.toWriterT(value).flatMap(map.andThen(WriterTOf::toWriterT));
  }
}

interface WriterTMonadWriter<F extends Kind<F, ?>, L>
    extends MonadWriter<WriterT<F, L, ?>, L>, WriterTMonad<F, L> {

  static <F extends Kind<F, ?>, L> MonadWriter<WriterT<F, L, ?>, L> instance(Monoid<L> monoid, Monad<F> monadF) {
    return new WriterTMonadWriter<>() {

      @Override
      public Monoid<L> monoid() {
        return monoid;
      }

      @Override
      public Monad<F> monadF() {
        return monadF;
      }
    };
  }

  @Override
  default <A> WriterT<F, L, A> writer(Tuple2<L, A> value) {
    return WriterT.lift(monoid(), monadF(), value);
  }

  @Override
  default <A> WriterT<F, L, Tuple2<L, A>> listen(Kind<WriterT<F, L, ?>, ? extends A> value) {
    return value.fix(WriterTOf::<F, L, A>toWriterT).listen();
  }

  @Override
  default <A> WriterT<F, L, A> pass(
      Kind<WriterT<F, L, ?>, Tuple2<Operator1<L>, A>> value) {
    WriterT<F, L, Tuple2<Operator1<L>, A>> writerT = value.fix(WriterTOf::toWriterT);
    return writerT.listen().flatMap((Tuple2<L, Tuple2<Operator1<L>, A>> tuple) -> {
        Operator1<L> operator = tuple.get2().get1();
        A value2 = tuple.get2().get2();
      return writer(Tuple.of(operator.apply(tuple.get1()), value2)).fix(WriterTOf::toWriterT);
      });
  }
}

interface WriterTMonadError<F extends Kind<F, ?>, L, E>
    extends MonadError<WriterT<F, L, ?>, E>, WriterTMonad<F, L> {

  static <F extends Kind<F, ?>, L, E> MonadError<WriterT<F, L, ?>, E> instance(
      Monoid<L> monoid, MonadError<F, E> monadErrorF) {
    return new WriterTMonadError<>() {

      @Override
      public Monoid<L> monoid() {
        return monoid;
      }

      @Override
      public MonadError<F, E> monadF() {
        return monadErrorF;
      }
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
      Kind<WriterT<F, L, ?>, A> value,
      Function1<? super E, ? extends Kind<WriterT<F, L, ?>, ? extends A>> handler) {
    return WriterT.writer(monoid(), monadF(),
        monadF().handleErrorWith(value.fix(WriterTOf::<F, L, A>toWriterT).value(),
            error -> handler.apply(error).fix(WriterTOf::<F, L, A>toWriterT).value()));
  }
}
