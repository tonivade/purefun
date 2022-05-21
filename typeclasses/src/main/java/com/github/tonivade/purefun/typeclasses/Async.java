/*
 * Copyright (c) 2018-2022, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.Witness;
import com.github.tonivade.purefun.type.Try;

public interface Async<F extends Witness> extends MonadDefer<F> {

  <A> Kind<F, A> asyncF(Function1<Consumer1<? super Try<? extends A>>, Kind<F, Unit>> consumer);
  
  default <A> Kind<F, A> async(Consumer1<Consumer1<? super Try<? extends A>>> consumer) {
    return asyncF(consumer.asFunction().andThen(this::pure));
  }
}
