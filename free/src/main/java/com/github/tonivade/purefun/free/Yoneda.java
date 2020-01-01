/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.free;

import static com.github.tonivade.purefun.Function1.identity;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.typeclasses.Functor;

@HigherKind
@FunctionalInterface
public interface Yoneda<F extends Kind, A> {

  <B> Higher1<F, B> apply(Function1<A, B> map);
  
  default Higher1<F, A> lower() {
    return apply(identity());
  }
  
  default <B> Yoneda<F, B> map(Function1<A, B> outer) {
    return new Yoneda<F, B>() {
      @Override
      public <C> Higher1<F, C> apply(Function1<B, C> inner) {
        return Yoneda.this.apply(outer.andThen(inner));
      }
    };
  }
  
  static <F extends Kind, A> Yoneda<F, A> of(Higher1<F, A> value, Functor<F> functor) {
    return new Yoneda<F, A>() {
      @Override
      public <B> Higher1<F, B> apply(Function1<A, B> map) {
        return functor.map(value, map);
      }
    };
  }
}
