/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Kind;

public interface Traverse<F extends Kind> {

  <G extends Kind, T, R> Higher1<G, Higher1<F, R>> traverse(Applicative<G> applicative, Higher1<F, T> value, Function1<T, ? extends Higher1<G, R>> mapper);
}
