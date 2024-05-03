/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.core.Precondition.checkNonNull;

import com.github.tonivade.purefun.Kind;

import com.github.tonivade.purefun.core.Unit;

public interface Fiber<F, A> {

  Kind<F, A> join();
  
  Kind<F, Unit> cancel();
  
  default <G> Fiber<G, A> mapK(FunctionK<F, G> map) {
    return of(map.apply(join()), map.apply(cancel()));
  }
  
  static <F, A> Fiber<F, A> of(Kind<F, A> join, Kind<F, Unit> cancel) {
    checkNonNull(join);
    checkNonNull(cancel);
    return new Fiber<>() {
      @Override
      public Kind<F, A> join() { return join; }
      
      @Override
      public Kind<F, Unit> cancel() { return cancel; }
    };
  }
}
