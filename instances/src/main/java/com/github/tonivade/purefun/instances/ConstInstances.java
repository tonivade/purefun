/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import com.github.tonivade.purefun.Eq;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Instance;
import com.github.tonivade.purefun.monad.Const;
import com.github.tonivade.purefun.typeclasses.Contravariant;
import com.github.tonivade.purefun.typeclasses.Functor;

public interface ConstInstances {

  static <T, A> Eq<Higher1<Higher1<Const.µ, T>, A>> eq(Eq<T> eq) {
    return (a, b) -> eq.eqv(a.fix1(Const::narrowK).get(), a.fix1(Const::narrowK).get());
  }

  static <T> Functor<Higher1<Const.µ, T>> functor() {
    return new ConstFunctor<T>() {};
  }

  static <T> Contravariant<Higher1<Const.µ, T>> contravariant() {
    return new ConstContravariant<T>() {};
  }
}

@Instance
interface ConstFunctor<T> extends Functor<Higher1<Const.µ, T>> {

  @Override
  default <A, B> Const<T, B> map(Higher1<Higher1<Const.µ, T>, A> value, Function1<A, B> map) {
    return value.fix1(Const::narrowK).retag();
  }
}

@Instance
interface ConstContravariant<T> extends Contravariant<Higher1<Const.µ, T>> {

  @Override
  default <A, B> Const<T, B> contramap(Higher1<Higher1<Const.µ, T>, A> value, Function1<B, A> map) {
    return value.fix1(Const::narrowK).retag();
  }
}
