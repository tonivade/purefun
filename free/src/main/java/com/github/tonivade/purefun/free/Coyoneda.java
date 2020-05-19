/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.free;

import static com.github.tonivade.purefun.Precondition.checkNonNull;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.typeclasses.Functor;

@HigherKind
public final class Coyoneda<F extends Kind, A, B> implements CoyonedaOf<F, A, B> {

  private final Higher1<F, A> value;
  private final Function1<A, B> map;

  private Coyoneda(Higher1<F, A> value, Function1<A, B> map) {
    this.value = checkNonNull(value);
    this.map = checkNonNull(map);
  }

  public Higher1<F, B> run(Functor<F> functor) {
    return functor.map(value, map);
  }

  public <C> Coyoneda<F, A, C> map(Function1<B, C> next) {
    return new Coyoneda<>(value, map.andThen(next));
  }

  public static <F extends Kind, A, B> Coyoneda<F, A, B> of(Higher1<F, A> value, Function1<A, B> map) {
    return new Coyoneda<>(value, map);
  }
}
