/*
 * Copyright (c) 2018-2023, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.annotation.Kind;
import com.github.tonivade.purefun.annotation.Witness;

public interface Comonad<F extends Witness> extends Functor<F> {

  <A, B> Kind<F, B> coflatMap(Kind<F, ? extends A> value, Function1<? super Kind<F, ? extends A>, ? extends B> map);

  <A> A extract(Kind<F, ? extends A> value);

  default <A> Kind<F, Kind<F, A>> coflatten(Kind<F, ? extends A> value) {
    return coflatMap(value, Kind::narrowK);
  }
}
