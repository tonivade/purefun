/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.free;

import com.github.tonivade.purefun.Kind;

import static com.github.tonivade.purefun.core.Precondition.checkNonNull;

import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Witness;
import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Mappable;
import com.github.tonivade.purefun.typeclasses.Functor;

@HigherKind
public final class Coyoneda<F extends Witness, A, B> implements CoyonedaOf<F, A, B>, Mappable<Kind<Kind<Coyoneda_, F>, A>, B> {

  private final Kind<F, ? extends A> value;
  private final Function1<? super A, ? extends B> map;

  private Coyoneda(Kind<F, ? extends A> value, Function1<? super A, ? extends B> map) {
    this.value = checkNonNull(value);
    this.map = checkNonNull(map);
  }

  public Kind<F, B> run(Functor<F> functor) {
    return functor.map(value, map);
  }

  @Override
  public <C> Coyoneda<F, A, C> map(Function1<? super B, ? extends C> next) {
    return new Coyoneda<>(value, map.andThen(next));
  }

  public static <F extends Witness, A, B> Coyoneda<F, A, B> of(Kind<F, ? extends A> value, Function1<? super A, ? extends B> map) {
    return new Coyoneda<>(value, map);
  }
}
