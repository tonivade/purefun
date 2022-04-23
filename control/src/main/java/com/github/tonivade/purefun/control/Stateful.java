/*
 * Copyright (c) 2018-2021, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.control;

import com.github.tonivade.purefun.Function1;

public abstract class Stateful<R, S, E> extends StateMarker implements Handler<R, E> {

  private final Field<S> state;

  protected Stateful(S init) {
    this.state = field(init);
  }

  public <T> Control<T> useState(Function1<S, Function1<Function1<T, Function1<S, Control<R>>>, Control<R>>> body) {
    return this.use(resume ->
        state.get().flatMap(before ->
            body.apply(before).apply(value -> after -> state.set(after).andThen(resume.apply(value)))
        )
    );
  }
}
