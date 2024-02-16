/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.generic;

import com.github.tonivade.purefun.core.Tuple;
import com.github.tonivade.purefun.core.Tuple2;
import com.github.tonivade.purefun.generic.HList.HCons;
import com.github.tonivade.purefun.generic.HList.HNil;

@FunctionalInterface
public interface HFoldr<T, V, L extends HList<L>, R> {

  R foldr(T value, V initialValue, L list);

  static <E, V> HFoldr<E, V, HNil, V> foldr() {
    return (head, value, list) -> value;
  }

  static <F, E, V, L extends HList<L>, R, X> HFoldr<E, V, HCons<F, L>, X>
      foldr(HApply<E, Tuple2<F, R>, X> apply, HFoldr<E, V, L, R> foldr) {
    return (head, value, list) ->
      apply.apply(head, Tuple.of(list.head(), foldr.foldr(head, value, list.tail())));
  }
}
