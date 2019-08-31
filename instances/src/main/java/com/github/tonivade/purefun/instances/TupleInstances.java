/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Higher2;
import com.github.tonivade.purefun.Instance;
import com.github.tonivade.purefun.Tuple1;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.typeclasses.Bifunctor;
import com.github.tonivade.purefun.typeclasses.Functor;

public interface TupleInstances {

  static Functor<Tuple1.µ> functor() {
    return new Tuple1Functor() {};
  }

  static Bifunctor<Tuple2.µ> bifunctor() {
    return new Tuple2Bifunctor() {};
  }
}

@Instance
interface Tuple1Functor extends Functor<Tuple1.µ> {

  @Override
  default <T, R> Higher1<Tuple1.µ, R> map(Higher1<Tuple1.µ, T> value, Function1<T, R> map) {
    return value.fix1(Tuple1::<T>narrowK).map(map).kind1();
  }
}

@Instance
interface Tuple2Bifunctor extends Bifunctor<Tuple2.µ> {

  @Override
  default <A, B, C, D> Higher2<Tuple2.µ, C, D> bimap(Higher2<Tuple2.µ, A, B> value,
                                          Function1<A, C> leftMap,
                                          Function1<B, D> rightMap) {
    return value.fix2(Tuple2::<A, B>narrowK).map(leftMap, rightMap).kind2();
  }
}
