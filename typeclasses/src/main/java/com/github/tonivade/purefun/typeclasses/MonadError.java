/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Matcher1;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.TypeClass;

@TypeClass
public interface MonadError<F extends Kind, E> extends ApplicativeError<F, E>, Monad<F> {

  default <A> Higher1<F, A> ensure(Higher1<F, A> value, Producer<E> error, Matcher1<A> matcher) {
    return flatMap(value, a -> matcher.match(a) ? pure(a) : raiseError(error.get()));
  }
}
