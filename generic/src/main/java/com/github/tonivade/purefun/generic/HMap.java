/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.generic;

import com.github.tonivade.purefun.Tuple;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.generic.HList.HCons;
import com.github.tonivade.purefun.generic.HList.HNil;

@FunctionalInterface
public interface HMap<E, L extends HList<L>, X extends HList<X>> {

  X map(E head, L list);

  static <F> HMap<F, HNil, HNil> map() {
    return (head, list) -> list;
  }

  static <F, E, R, L extends HList<L>, X extends HList<X>> HMap<F, HCons<E, L>, HCons<R, X>>
      map(HApply<F, Tuple2<E, X>, HCons<R, X>> apply, HMap<F, L, X> mapper) {
    return (head, list) -> apply.apply(head, Tuple.of(list.head(), mapper.map(head, list.tail())));
  }
}
