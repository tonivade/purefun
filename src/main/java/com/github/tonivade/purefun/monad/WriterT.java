/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.monad;

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
  
  @Override
  public <R> WriterT<F, L, R> map(Function1<A, R> mapper) {
    return new WriterT<>(monoid, monad, monad.map(value, writer -> writer.map2(mapper)));
  }
  
  @Override
  public <R> WriterT<F, L, R> flatMap(Function1<A, ? extends Higher3<WriterT.µ, F, L, R>> mapper) {
    return new WriterT<>(monoid, monad, 
        monad.flatMap(value, 
            self -> monad.map(mapper.andThen(WriterT::narrowK).apply(self.get2()).value, 
                other -> Tuple.of(monoid.combine(other.get1(), self.get1()), other.get2()))));
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
