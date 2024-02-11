/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.control;

import com.github.tonivade.purefun.core.PartialFunction1;

public interface Marker {

  interface Cont<R> {}

  interface State<S> {

    S backup();

    void restore(S value);
  }

  interface Catch<R> {

    PartialFunction1<Throwable, Control<R>> handle();
  }
}
