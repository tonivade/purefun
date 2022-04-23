/*
 * Copyright (c) 2018-2021, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.transformer;

import static com.github.tonivade.purefun.Function1.cons;
import static com.github.tonivade.purefun.Function1.identity;
import static com.github.tonivade.purefun.Precondition.checkNonNull;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Bindable;
import com.github.tonivade.purefun.Tuple;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.Witness;
import com.github.tonivade.purefun.typeclasses.FunctionK;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.Monoid;

@HigherKind
public non-sealed interface WriterT<F extends Witness, L, A> extends WriterTOf<F, L, A>, Bindable<Kind<Kind<WriterT_, F>, L>, A> {

  Monoid<L> monoid();
  Monad<F> monad();
  Kind<F, Tuple2<L, A>> value();

  default Kind<F, A> getValue() {
    return monad().map(value(), Tuple2::get2);
  }

  default Kind<F, L> getLog() {
    return monad().map(value(), Tuple2::get1);
  }

  @Override
  default <R> WriterT<F, L, R> map(Function1<? super A, ? extends R> mapper) {
    return bimap(monoid(), identity(), mapper);
  }

  default <V> WriterT<F, V, A> mapLog(Monoid<V> monoidV, Function1<? super L, ? extends V> mapper) {
    return bimap(monoidV, mapper, identity());
  }

  default WriterT<F, L, Tuple2<L, A>> listen() {
    return writer(monoid(), monad(), monad().map(value(),
        tuple -> Tuple.of(tuple.get1(), Tuple.of(tuple.get1(), tuple.get2()))));
  }

  default WriterT<F, L, A> append(L log2) {
    return mapLog(monoid(), log1 -> monoid().combine(log1, log2));
  }

  default WriterT<F, L, A> reset() {
    return bimap(monoid(), cons(monoid().zero()), identity());
  }

  default <V, R> WriterT<F, V, R> bimap(Monoid<V> monoidV, 
      Function1<? super L, ? extends V> mapper1, Function1<? super A, ? extends R> mapper2) {
    return writer(monoidV, monad(), monad().map(value(), tuple -> tuple.map(mapper1, mapper2)));
  }

  default <G extends Witness> WriterT<G, L, A> mapK(Monad<G> monadG, FunctionK<F, G> functionK) {
    return writer(monoid(), monadG, functionK.apply(value()));
  }

  @Override
  default <R> WriterT<F, L, R> flatMap(Function1<? super A, ? extends Kind<Kind<Kind<WriterT_, F>, L>, ? extends R>> mapper) {
    return writer(monoid(), monad(),
        monad().flatMap(value(),
            current -> monad().map(mapper.andThen(WriterTOf::<F, L, R>narrowK).apply(current.get2()).value(),
                other -> Tuple.of(monoid().combine(current.get1(), other.get1()), other.get2()))));
  }

  static <F extends Witness, L, A> WriterT<F, L, A> pure(Monoid<L> monoid, Monad<F> monad, A value) {
    return lift(monoid, monad, Tuple2.of(monoid.zero(), value));
  }

  static <F extends Witness, L, A> WriterT<F, L, A> lift(Monoid<L> monoid, Monad<F> monad, Tuple2<L, A> value) {
    return writer(monoid, monad, monad.pure(value));
  }

  static <F extends Witness, L, A> WriterT<F, L, A> writer(Monoid<L> monoid, Monad<F> monad, Kind<F, Tuple2<L, A>> value) {
    checkNonNull(monoid);
    checkNonNull(monad);
    checkNonNull(value);
    return new WriterT<>() {

      @Override
      public Monoid<L> monoid() { return monoid; }

      @Override
      public Monad<F> monad() { return monad; }

      @Override
      public Kind<F, Tuple2<L, A>> value() { return value; }
    };
  }
}
