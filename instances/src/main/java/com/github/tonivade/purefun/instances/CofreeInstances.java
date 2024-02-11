/*
 * Copyright (c) 2018-2023, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Witness;
import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.free.Cofree;
import com.github.tonivade.purefun.free.CofreeOf;
import com.github.tonivade.purefun.free.Cofree_;
import com.github.tonivade.purefun.typeclasses.Comonad;
import com.github.tonivade.purefun.typeclasses.Functor;

@SuppressWarnings("unchecked")
public interface CofreeInstances {

  static <F extends Witness> Functor<Kind<Cofree_, F>> functor() {
    return CofreeFunctor.INSTANCE;
  }

  static <F extends Witness> Comonad<Kind<Cofree_, F>> comonad() {
    return CofreeComonad.INSTANCE;
  }
}

interface CofreeFunctor<F extends Witness> extends Functor<Kind<Cofree_, F>> {

  @SuppressWarnings("rawtypes")
  CofreeFunctor INSTANCE = new CofreeFunctor() {};

  @Override
  default <T, R> Cofree<F, R> map(Kind<Kind<Cofree_, F>, ? extends T> value, Function1<? super T, ? extends R> map) {
    return value.fix(CofreeOf::narrowK).map(map);
  }
}

interface CofreeComonad<F extends Witness> extends Comonad<Kind<Cofree_, F>>, CofreeFunctor<F> {

  @SuppressWarnings("rawtypes")
  CofreeComonad INSTANCE = new CofreeComonad() { };

  @Override
  default <A> A extract(Kind<Kind<Cofree_, F>, ? extends A> value) {
    return value.fix(CofreeOf::narrowK).extract();
  }

  @Override
  default <A, B> Cofree<F, B> coflatMap(
      Kind<Kind<Cofree_, F>, ? extends A> value, Function1<? super Kind<Kind<Cofree_, F>, ? extends A>, ? extends B> map) {
    return value.fix(CofreeOf::narrowK).coflatMap(map::apply);
  }
}
