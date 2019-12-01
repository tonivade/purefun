/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import com.github.tonivade.purefun.Eq;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Higher2;
import com.github.tonivade.purefun.Instance;
import com.github.tonivade.purefun.type.Const;
import com.github.tonivade.purefun.typeclasses.Contravariant;
import com.github.tonivade.purefun.typeclasses.Functor;

public interface ConstInstances {

  static <T, A> Eq<Higher1<Higher1<Const.µ, T>, A>> eq(Eq<T> eq) {
    return (a, b) -> eq.eqv(a.fix1(Const::narrowK).get(), a.fix1(Const::<T, A>narrowK).get());
  }

  static <T> Functor<Higher1<Const.µ, T>> functor() {
    return (ConstFunctor<T>) ConstFunctor.INSTANCE;
  }

  static <T> Contravariant<Higher1<Const.µ, T>> contravariant() {
    return (ConstContravariant<T>) ConstContravariant.INSTANCE;
  }
}

@Instance
interface ConstFunctor<T> extends Functor<Higher1<Const.µ, T>> {

  ConstFunctor<?> INSTANCE = new ConstFunctor() { };

  @Override
  default <A, B> Higher2<Const.µ, T, B> map(Higher1<Higher1<Const.µ, T>, A> value, Function1<A, B> map) {
    return value.fix1(Const::narrowK).<B>retag().kind2();
  }
}

@Instance
interface ConstContravariant<T> extends Contravariant<Higher1<Const.µ, T>> {

  ConstContravariant<?> INSTANCE = new ConstContravariant<Object>() { };

  @Override
  default <A, B> Higher2<Const.µ, T, B> contramap(Higher1<Higher1<Const.µ, T>, A> value, Function1<B, A> map) {
    return value.fix1(Const::narrowK).<B>retag().kind2();
  }
}
