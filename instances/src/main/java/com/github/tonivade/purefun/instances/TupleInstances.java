/*
 * Copyright (c) 2018-2021, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Tuple1Of;
import com.github.tonivade.purefun.Tuple1_;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.Tuple2Of;
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
  default <T, R> Kind<Tuple1_, R> map(Kind<Tuple1_, ? extends T> value, Function1<? super T, ? extends R> map) {
    return value.fix(Tuple1Of::narrowK).map(map);
  }
}

interface Tuple2Bifunctor extends Bifunctor<Tuple2_> {

  Tuple2Bifunctor INSTANCE = new Tuple2Bifunctor() {};

  @Override
  default <A, B, C, D> Tuple2<C, D> bimap(Kind<Kind<Tuple2_, A>, ? extends B> value,
                                          Function1<? super A, ? extends C> leftMap,
                                          Function1<? super B, ? extends D> rightMap) {
    return value.fix(Tuple2Of::narrowK).map(leftMap, rightMap);
  }
}
