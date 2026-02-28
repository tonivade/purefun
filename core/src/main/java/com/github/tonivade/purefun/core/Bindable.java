/*
 * Copyright (c) 2018-2026, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.core;

import com.github.tonivade.purefun.Kind;

public interface Bindable<F extends Bindable<F, ?>, A> extends Mappable<F, A> {

  @Override
  <R> Bindable<F, R> map(Function1<? super A, ? extends R> mapper);

  <R> Bindable<F, R> flatMap(Function1<? super A, ? extends Kind<F, ? extends R>> mapper);

  default <R> Bindable<F, R> andThen(Kind<F, ? extends R> next) {
    return flatMap(ignore -> next);
  }

  @SuppressWarnings("unchecked")
  static <F extends Bindable<F, ?>, A> Bindable<F, A> narrowK(Kind<F, ? extends A> kind) {
    return (Bindable<F, A>) kind;
  }
}
