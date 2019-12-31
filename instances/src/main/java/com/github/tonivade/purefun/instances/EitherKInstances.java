/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.free.EitherK;
import com.github.tonivade.purefun.typeclasses.InjectK;

public interface EitherKInstances {

  static <F extends Kind, X extends Kind>
      InjectK<F, Higher1<Higher1<EitherK.µ, F>, X>> injectEitherKLeft() {
    return new InjectK<F, Higher1<Higher1<EitherK.µ, F>, X>>() {
      @Override
      public <T> Higher1<Higher1<Higher1<EitherK.µ, F>, X>, T> inject(Higher1<F, T> value) {
        return EitherK.<F, X, T>left(value).kind1();
      }
    };
  }

  static <F extends Kind, R extends Kind, X extends Kind>
      InjectK<F, Higher1<Higher1<EitherK.µ, X>, R>> injectEitherKRight(InjectK<F, R> inject) {
    return new InjectK<F, Higher1<Higher1<EitherK.µ, X>, R>>() {
      @Override
      public <T> Higher1<Higher1<Higher1<EitherK.µ, X>, R>, T> inject(Higher1<F, T> value) {
        return EitherK.<X, R, T>right(inject.inject(value)).kind1();
      }
    };
  }
}
