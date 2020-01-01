/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.Function1.identity;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.TypeClass;

@TypeClass
public interface Comonad<F extends Kind> extends Functor<F> {

  <A, B> Higher1<F, B> coflatMap(Higher1<F, A> value, Function1<Higher1<F, A>, B> map);

  <A> A extract(Higher1<F, A> value);

  default <A> Higher1<F, Higher1<F, A>> coflatten(Higher1<F, A> value) {
    return coflatMap(value, identity());
  }
}
