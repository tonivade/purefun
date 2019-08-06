/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher2;
import com.github.tonivade.purefun.Instance;
import com.github.tonivade.purefun.typeclasses.Profunctor;

public interface Function1Instances {

  static Profunctor<Function1.µ> profunctor() {
    return new Function1Profunctor() {};
  }
}

@Instance
interface Function1Profunctor extends Profunctor<Function1.µ> {
  @Override
  default <A, B, C, D> Higher2<Function1.µ, C, D> dimap(Higher2<Function1.µ, A, B> value, Function1<C, A> contramap, Function1<B, D> map) {
    Function1<A, B> function = value.fix2(Function1::narrowK);
    return function.compose(contramap).andThen(map);
  }
}
