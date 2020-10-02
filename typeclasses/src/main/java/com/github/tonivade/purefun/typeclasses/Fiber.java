package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.Witness;

public interface Fiber<F extends Witness, A> {

  Kind<F, A> join();
  
  Kind<F, Unit> cancel();
  
  default <G extends Witness> Fiber<G, A> mapK(FunctionK<F, G> map) {
    return of(() -> map.apply(join()), () -> map.apply(cancel()));
  }
  
  static <F extends Witness, A> Fiber<F, A> of(Producer<Kind<F, A>> join, Producer<Kind<F, Unit>> cancel) {
    return new Fiber<F, A>() {
      @Override
      public Kind<F, A> join() { return join.get(); }
      
      @Override
      public Kind<F, Unit> cancel() { return cancel.get(); }
    };
  }
}
