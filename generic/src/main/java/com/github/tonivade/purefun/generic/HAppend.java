/*
 * Copyright (c) 2018-2022, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.generic;

import com.github.tonivade.purefun.generic.HList.HCons;
import com.github.tonivade.purefun.generic.HList.HNil;

@FunctionalInterface
public interface HAppend<L extends HList<L>, R extends HList<R>, X extends HList<X>> {

  X append(L left, R right);

  static <L extends HList<L>> HAppend<HNil, L, L> append() {
    return (empty, right) -> right;
  }

  static <E, L extends HList<L>, R extends HList<R>, X extends HList<X>>
      HAppend<HCons<E, L>, R, HCons<E, X>> append(HAppend<L, R, X> append) {
    return (left, right) -> HList.cons(left.head(), append.append(left.tail(), right));
  }
}
