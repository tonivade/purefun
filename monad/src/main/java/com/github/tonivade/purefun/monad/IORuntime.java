/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.monad;

import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Kind;

public interface IORuntime<F extends Kind> {

  <A> Higher1<F, A> run(IO<A> effect);
}
