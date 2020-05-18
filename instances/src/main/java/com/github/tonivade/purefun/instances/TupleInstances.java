/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Higher2;
import com.github.tonivade.purefun.Tuple1_;
import com.github.tonivade.purefun.Tuple2_;
import com.github.tonivade.purefun.typeclasses.Bifunctor;
import com.github.tonivade.purefun.typeclasses.Functor;

public interface TupleInstances {

  static Functor<Tuple1_> functor() {
    return Tuple1Functor.INSTANCE;
  }

  static Bifunctor<Tuple2_> bifunctor() {
    return Tuple2Bifunctor.INSTANCE;
  }
}

interface Tuple1Functor extends Functor<Tuple1_> {

  Tuple1Functor INSTANCE = new Tuple1Functor() {};

  @Override
  default <T, R> Higher1<Tuple1_, R> map(Higher1<Tuple1_, T> value, Function1<T, R> map) {
    return value.fix1(Tuple1_::narrowK).map(map);
  }
}

interface Tuple2Bifunctor extends Bifunctor<Tuple2_> {

  Tuple2Bifunctor INSTANCE = new Tuple2Bifunctor() {};

  @Override
  default <A, B, C, D> Higher2<Tuple2_, C, D> bimap(Higher2<Tuple2_, A, B> value,
                                          Function1<A, C> leftMap,
                                          Function1<B, D> rightMap) {
    return value.fix2(Tuple2_::narrowK).map(leftMap, rightMap);
  }
}
