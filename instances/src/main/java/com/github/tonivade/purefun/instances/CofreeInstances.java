/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Higher2;
import com.github.tonivade.purefun.Instance;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.free.Cofree;
import com.github.tonivade.purefun.typeclasses.Comonad;
import com.github.tonivade.purefun.typeclasses.Functor;

public interface CofreeInstances {

  static <F extends Kind> Functor<Higher1<Cofree.µ, F>> functor() {
    return CofreeFunctor.instance();
  }

  static <F extends Kind> Comonad<Higher1<Cofree.µ, F>> comonad() {
    return CofreeComonad.instance();
  }
}

@Instance
interface CofreeFunctor<F extends Kind> extends Functor<Higher1<Cofree.µ, F>> {

  @Override
  default <T, R> Higher2<Cofree.µ, F, R> map(Higher1<Higher1<Cofree.µ, F>, T> value, Function1<T, R> map) {
    return value.fix1(Cofree::narrowK).map(map).kind2();
  }
}

@Instance
interface CofreeComonad<F extends Kind> extends Comonad<Higher1<Cofree.µ, F>>, CofreeFunctor<F> {

  @Override
  default <A> A extract(Higher1<Higher1<Cofree.µ, F>, A> value) {
    return value.fix1(Cofree::narrowK).extract();
  }

  @Override
  default <A, B> Higher2<Cofree.µ, F, B> coflatMap(
      Higher1<Higher1<Cofree.µ, F>, A> value, Function1<Higher1<Higher1<Cofree.µ, F>, A>, B> map) {
    return value.fix1(Cofree::narrowK).coflatMap(c -> map.apply(c.kind1())).kind2();
  }
}
