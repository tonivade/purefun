/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import com.github.tonivade.purefun.Kind;

import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.free.Cofree;
import com.github.tonivade.purefun.free.CofreeOf;
import com.github.tonivade.purefun.typeclasses.Comonad;
import com.github.tonivade.purefun.typeclasses.Functor;

@SuppressWarnings("unchecked")
public interface CofreeInstances {

  static <F> Functor<Kind<Cofree<?, ?>, F>> functor() {
    return CofreeFunctor.INSTANCE;
  }

  static <F> Comonad<Kind<Cofree<?, ?>, F>> comonad() {
    return CofreeComonad.INSTANCE;
  }
}

interface CofreeFunctor<F> extends Functor<Kind<Cofree<?, ?>, F>> {

  @SuppressWarnings("rawtypes")
  CofreeFunctor INSTANCE = new CofreeFunctor() {};

  @Override
  default <T, R> Cofree<F, R> map(Kind<Kind<Cofree<?, ?>, F>, ? extends T> value, Function1<? super T, ? extends R> map) {
    return value.fix(CofreeOf::narrowK).map(map);
  }
}

interface CofreeComonad<F> extends Comonad<Kind<Cofree<?, ?>, F>>, CofreeFunctor<F> {

  @SuppressWarnings("rawtypes")
  CofreeComonad INSTANCE = new CofreeComonad() { };

  @Override
  default <A> A extract(Kind<Kind<Cofree<?, ?>, F>, ? extends A> value) {
    return value.fix(CofreeOf::narrowK).extract();
  }

  @Override
  default <A, B> Cofree<F, B> coflatMap(
      Kind<Kind<Cofree<?, ?>, F>, ? extends A> value, Function1<? super Kind<Kind<Cofree<?, ?>, F>, ? extends A>, ? extends B> map) {
    return value.fix(CofreeOf::narrowK).coflatMap(map);
  }
}
