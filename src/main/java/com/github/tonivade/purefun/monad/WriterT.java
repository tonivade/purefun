/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.monad;

import static com.github.tonivade.purefun.Function1.cons;
import static com.github.tonivade.purefun.Function1.identity;
import static java.util.Objects.requireNonNull;

import com.github.tonivade.purefun.FlatMap3;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Higher2;
import com.github.tonivade.purefun.Higher3;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Tuple;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.Monoid;
import com.github.tonivade.purefun.typeclasses.Transformer;

public class WriterT<F extends Kind, L, A> implements FlatMap3<WriterT.µ, F, L, A> {

  public static final class µ implements Kind {}

  private final Monoid<L> monoid;
  private final Monad<F> monad;
  private final Higher1<F, Tuple2<L, A>> value;

  private WriterT(Monoid<L> monoid, Monad<F> monad, Higher1<F, Tuple2<L, A>> value) {
    this.monoid = requireNonNull(monoid);
    this.monad = requireNonNull(monad);
    this.value = requireNonNull(value);
  }

  public Higher1<F, A> getValue() {
    return monad.map(value, Tuple2::get2);
  }

  public Higher1<F, L> getLog() {
    return monad.map(value, Tuple2::get1);
  }

  public WriterT<F, L, A> append(L l2) {
    return mapLog(monoid, l1 -> monoid.combine(l1, l2));
  }

  @Override
  public <R> WriterT<F, L, R> map(Function1<A, R> mapper) {
    return bimap(monoid, identity(), mapper);
  }

  public <V> WriterT<F, V, A> mapLog(Monoid<V> monoidV, Function1<L, V> mapper) {
    return bimap(monoidV, mapper, identity());
  }

  public WriterT<F, L, A> reset() {
    return bimap(monoid, cons(monoid.zero()), identity());
  }

  public <V, R> WriterT<F, V, R> bimap(Monoid<V> monoidV, Function1<L, V> mapper1, Function1<A, R> mapper2) {
    return writer(monoidV, monad, monad.map(value, tuple -> tuple.map(mapper1, mapper2)));
  }

  public <G extends Kind> WriterT<G, L, A> mapK(Monad<G> monadG, Transformer<F, G> transformer) {
    return writer(monoid, monadG, transformer.apply(value));
  }

  @Override
  public <R> WriterT<F, L, R> flatMap(Function1<A, ? extends Higher3<WriterT.µ, F, L, R>> mapper) {
    return writer(monoid, monad,
        monad.flatMap(value,
            current -> monad.map(mapper.andThen(WriterT::narrowK).apply(current.get2()).value,
                other -> Tuple.of(monoid.combine(other.get1(), current.get1()), other.get2()))));
  }

  public static <F extends Kind, L, A> WriterT<F, L, A> pure(Monoid<L> monoid, Monad<F> monad, A value) {
    return lift(monoid, monad, Tuple2.of(monoid.zero(), value));
  }

  public static <F extends Kind, L, A> WriterT<F, L, A> lift(Monoid<L> monoid, Monad<F> monad, Tuple2<L, A> value) {
    return writer(monoid, monad, monad.pure(value));
  }

  public static <F extends Kind, L, A> WriterT<F, L, A> writer(Monoid<L> monoid, Monad<F> monad, Higher1<F, Tuple2<L, A>> value) {
    return new WriterT<>(monoid, monad, value);
  }

  public static <F extends Kind, L> Monad<Higher1<Higher1<WriterT.µ, F>, L>> monad(Monoid<L> monoid, Monad<F> monadF) {
    return new Monad<Higher1<Higher1<WriterT.µ, F>, L>>() {

      @Override
      public <T> WriterT<F, L, T> pure(T value) {
        return WriterT.pure(monoid, monadF, value);
      }

      @Override
      public <T, R> WriterT<F, L, R> flatMap(Higher1<Higher1<Higher1<WriterT.µ, F>, L>, T> value,
          Function1<T, ? extends Higher1<Higher1<Higher1<WriterT.µ, F>, L>, R>> map) {
        return WriterT.narrowK(value).flatMap(map.andThen(WriterT::narrowK));
      }
    };
  }

  public static <F extends Kind, L, A> WriterT<F, L, A> narrowK(Higher3<WriterT.µ, F, L, A> hkt) {
    return (WriterT<F, L, A>) hkt;
  }

  public static <F extends Kind, L, A> WriterT<F, L, A> narrowK(Higher2<Higher1<WriterT.µ, F>, L, A> hkt) {
    return (WriterT<F, L, A>) hkt;
  }

  @SuppressWarnings("unchecked")
  public static <F extends Kind, L, A> WriterT<F, L, A> narrowK(Higher1<Higher1<Higher1<WriterT.µ, F>, L>, A> hkt) {
    return (WriterT<F, L, A>) hkt;
  }
}
