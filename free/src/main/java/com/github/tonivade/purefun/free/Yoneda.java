/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.free;

import static com.github.tonivade.purefun.core.Function1.identity;
import static com.github.tonivade.purefun.core.Precondition.checkNonNull;

import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Witness;
import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Mappable;
import com.github.tonivade.purefun.typeclasses.Functor;

@HigherKind
public sealed interface Yoneda<F extends Witness, A> extends YonedaOf<F, A>, Mappable<Kind<Yoneda_, F>, A> {

  <B> Kind<F, B> apply(Function1<? super A, ? extends B> map);

  default Kind<F, A> lower() {
    return apply(identity());
  }

  @Override
  default <B> Yoneda<F, B> map(Function1<? super A, ? extends B> outer) {
    return new Mapped<>(Yoneda.this, outer);
  }

  static <F extends Witness, A> Yoneda<F, A> of(Kind<F, A> value, Functor<F> functor) {
    return new Impl<>(value, functor);
  }
  
  final class Impl<F extends Witness, A> implements Yoneda<F, A> {
    
    private final Kind<F, A> value;
    private final Functor<F> functor;
    
    private Impl(Kind<F, A> value, Functor<F> functor) {
      this.value = checkNonNull(value);
      this.functor = checkNonNull(functor);
    }

    @Override
    public <B> Kind<F, B> apply(Function1<? super A, ? extends B> map) {
      return functor.map(value, map);
    }
  }
  
  final class Mapped<F extends Witness, A, B> implements Yoneda<F, B> {

    private final Yoneda<F, A> self;
    private final Function1<? super A, ? extends B> outer;

    private Mapped(Yoneda<F, A> self, Function1<? super A, ? extends B> outer) {
      this.self = checkNonNull(self);
      this.outer = checkNonNull(outer);
    }
    
    @Override
    public <C> Kind<F, C> apply(Function1<? super B, ? extends C> inner) {
      return self.apply(outer.andThen(inner));
    }
  }
}
