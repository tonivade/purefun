/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.Kind;

import com.github.tonivade.purefun.core.Consumer1;
import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Unit;
import com.github.tonivade.purefun.type.Try;

public interface Async<F extends Kind<F, ?>> extends MonadDefer<F> {

  <A> Kind<F, A> asyncF(Function1<Consumer1<? super Try<? extends A>>, Kind<F, Unit>> consumer);

  default <A> Kind<F, A> async(Consumer1<Consumer1<? super Try<? extends A>>> consumer) {
    return asyncF(consumer.asFunction().andThen(this::pure));
  }
}
