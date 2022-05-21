/*
 * Copyright (c) 2018-2022, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.Unit.unit;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Witness;
import com.github.tonivade.purefun.Operator1;
import com.github.tonivade.purefun.Tuple;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.Unit;

public interface MonadWriter<F extends Witness, W> extends Monad<F> {

  <A> Kind<F, A> writer(Tuple2<W, A> value);
  <A> Kind<F, Tuple2<W, A>> listen(Kind<F, ? extends A> value);
  <A> Kind<F, A> pass(Kind<F, Tuple2<Operator1<W>, A>> value);

  default Kind<F, Unit> tell(W writer) {
    return writer(Tuple.of(writer, unit()));
  }

  default <A, B> Kind<F, Tuple2<B, A>> listens(Kind<F, ? extends A> value, Function1<? super W, ? extends B> mapper) {
    Kind<F, Tuple2<W, A>> listen = listen(value);
    return map(listen, tuple -> tuple.map1(mapper));
  }

  default <A> Kind<F, A> censor(Kind<F, ? extends A> value, Operator1<W> mapper) {
    return flatMap(listen(value), tuple -> writer(tuple.map1(mapper)));
  }
}
