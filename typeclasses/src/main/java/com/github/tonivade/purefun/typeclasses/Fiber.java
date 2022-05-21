/*
 * Copyright (c) 2018-2022, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.Precondition.checkNonNull;

import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.Witness;

public interface Fiber<F extends Witness, A> {

  Kind<F, A> join();
  
  Kind<F, Unit> cancel();
  
  default <G extends Witness> Fiber<G, A> mapK(FunctionK<F, G> map) {
    return of(map.apply(join()), map.apply(cancel()));
  }
  
  static <F extends Witness, A> Fiber<F, A> of(Kind<F, A> join, Kind<F, Unit> cancel) {
    checkNonNull(join);
    checkNonNull(cancel);
    return new Fiber<F, A>() {
      @Override
      public Kind<F, A> join() { return join; }
      
      @Override
      public Kind<F, Unit> cancel() { return cancel; }
    };
  }
}
