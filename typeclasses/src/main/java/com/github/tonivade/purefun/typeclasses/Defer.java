/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.Kind;

import com.github.tonivade.purefun.core.Producer;

public interface Defer<F> {

  <A> Kind<F, A> defer(Producer<? extends Kind<F, ? extends A>> defer);
}
