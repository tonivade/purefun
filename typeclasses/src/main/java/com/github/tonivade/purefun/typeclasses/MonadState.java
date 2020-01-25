/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Operator1;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.TypeClass;
import com.github.tonivade.purefun.Unit;

@TypeClass
public interface MonadState<F extends Kind, S> extends Monad<F> {
  Higher1<F, S> get();
  Higher1<F, Unit> set(S state);

  default Higher1<F, Unit> modify(Operator1<S> mapper) {
    return flatMap(get(), s -> set(mapper.apply(s)));
  }

  default <A> Higher1<F, A> inspect(Function1<S, A> mapper) {
    return map(get(), mapper);
  }

  default <A> Higher1<F, A> state(Function1<S, Tuple2<S, A>> mapper) {
    return flatMap(get(), s -> mapper.apply(s).applyTo((s1, a) -> map(set(s1), x -> a)));
  }
}
