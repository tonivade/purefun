/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import com.github.tonivade.purefun.Eq;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Higher2;
import com.github.tonivade.purefun.Instance;
import com.github.tonivade.purefun.type.Const;
import com.github.tonivade.purefun.typeclasses.Applicative;
import com.github.tonivade.purefun.typeclasses.Contravariant;
import com.github.tonivade.purefun.typeclasses.Functor;
import com.github.tonivade.purefun.typeclasses.Monoid;

public interface ConstInstances {

  static <T, A> Eq<Higher1<Higher1<Const.µ, T>, A>> eq(Eq<T> eq) {
    return (a, b) -> eq.eqv(a.fix1(Const::narrowK).get(), a.fix1(Const::narrowK).get());
  }

  static <T> Functor<Higher1<Const.µ, T>> functor() {
    return ConstFunctor.instance();
  }

  static <T> Applicative<Higher1<Const.µ, T>> applicative(Monoid<T> monoid) {
    return ConstApplicative.instance(monoid);
  }

  static <T> Contravariant<Higher1<Const.µ, T>> contravariant() {
    return ConstContravariant.instance();
  }
}

@Instance
interface ConstFunctor<T> extends Functor<Higher1<Const.µ, T>> {

  ConstFunctor<?> INSTANCE = new ConstFunctor<Object>() { };

  @SuppressWarnings("unchecked")
  static <T> ConstFunctor<T> instance() {
    return (ConstFunctor<T>) INSTANCE;
  }

  @Override
  default <A, B> Higher1<Higher1<Const.µ, T>, B> map(Higher1<Higher1<Const.µ, T>, A> value, Function1<A, B> map) {
    return value.fix1(Const::narrowK).<B>retag().kind2();
  }
}

@Instance
interface ConstApplicative<T> extends Applicative<Higher1<Const.µ, T>> {

  static <T> ConstApplicative<T> instance(Monoid<T> monoid) {
    return () -> monoid;
  }

  Monoid<T> monoid();

  @Override
  default <A> Higher2<Const.µ, T, A> pure(A value) {
    return Const.<T, A>of(monoid().zero()).kind2();
  }

  @Override
  default <A, B> Higher2<Const.µ, T, B> ap(
      Higher1<Higher1<Const.µ, T>, A> value, Higher1<Higher1<Const.µ, T>, Function1<A, B>> apply) {
    return Const.<T, B>of(monoid().combine(
            apply.fix1(Const::narrowK).<B>retag().get(),
            value.fix1(Const::narrowK).<B>retag().get())).kind2();
  }
}

@Instance
interface ConstContravariant<T> extends Contravariant<Higher1<Const.µ, T>> {

  ConstContravariant<?> INSTANCE = new ConstContravariant<Object>() { };

  @SuppressWarnings("unchecked")
  static <T> ConstContravariant<T> instance() {
    return (ConstContravariant<T>) INSTANCE;
  }

  @Override
  default <A, B> Higher2<Const.µ, T, B> contramap(Higher1<Higher1<Const.µ, T>, A> value, Function1<B, A> map) {
    return value.fix1(Const::narrowK).<B>retag().kind2();
  }
}
