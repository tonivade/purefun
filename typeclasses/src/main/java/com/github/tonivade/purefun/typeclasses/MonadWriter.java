/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Operator1;
import com.github.tonivade.purefun.Tuple;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.Unit;

import static com.github.tonivade.purefun.Unit.unit;

public interface MonadWriter<F extends Kind, W> extends Monad<F> {

  <A> Higher1<F, A> writer(Tuple2<W, A> value);
  <A> Higher1<F, Tuple2<W, A>> listen(Higher1<F, A> value);
  <A> Higher1<F, A> pass(Higher1<F, Tuple2<Operator1<W>, A>> value);

  default Higher1<F, Unit> tell(W writer) {
    return writer(Tuple.of(writer, unit()));
  }

  default <A, B> Higher1<F, Tuple2<B, A>> listens(Higher1<F, A> value, Function1<W, B> mapper) {
    return map(listen(value), tuple -> tuple.map1(mapper));
  }

  default <A> Higher1<F, A> censor(Higher1<F, A> value, Operator1<W> mapper) {
    return flatMap(listen(value), tuple -> writer(tuple.map1(mapper)));
  }
}
