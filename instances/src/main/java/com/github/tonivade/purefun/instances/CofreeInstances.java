/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Higher2;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.free.Cofree_;
import com.github.tonivade.purefun.typeclasses.Comonad;
import com.github.tonivade.purefun.typeclasses.Functor;

@SuppressWarnings("unchecked")
public interface CofreeInstances {

  static <F extends Kind> Functor<Higher1<Cofree_, F>> functor() {
    return CofreeFunctor.INSTANCE;
  }

  static <F extends Kind> Comonad<Higher1<Cofree_, F>> comonad() {
    return CofreeComonad.INSTANCE;
  }
}

interface CofreeFunctor<F extends Kind> extends Functor<Higher1<Cofree_, F>> {

  @SuppressWarnings("rawtypes")
  CofreeFunctor INSTANCE = new CofreeFunctor() {};

  @Override
  default <T, R> Higher2<Cofree_, F, R> map(Higher1<Higher1<Cofree_, F>, T> value, Function1<T, R> map) {
    return value.fix1(Cofree_::narrowK).map(map);
  }
}

interface CofreeComonad<F extends Kind> extends Comonad<Higher1<Cofree_, F>>, CofreeFunctor<F> {

  @SuppressWarnings("rawtypes")
  CofreeComonad INSTANCE = new CofreeComonad() { };

  @Override
  default <A> A extract(Higher1<Higher1<Cofree_, F>, A> value) {
    return value.fix1(Cofree_::narrowK).extract();
  }

  @Override
  default <A, B> Higher2<Cofree_, F, B> coflatMap(
      Higher1<Higher1<Cofree_, F>, A> value, Function1<Higher1<Higher1<Cofree_, F>, A>, B> map) {
    return value.fix1(Cofree_::narrowK).coflatMap(map::apply);
  }
}
