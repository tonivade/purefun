/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import com.github.tonivade.purefun.Eq;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Function2;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Witness;
import com.github.tonivade.purefun.type.Const;
import com.github.tonivade.purefun.type.ConstOf;
import com.github.tonivade.purefun.type.Const_;
import com.github.tonivade.purefun.type.Eval;
import com.github.tonivade.purefun.typeclasses.Applicative;
import com.github.tonivade.purefun.typeclasses.Contravariant;
import com.github.tonivade.purefun.typeclasses.Foldable;
import com.github.tonivade.purefun.typeclasses.Functor;
import com.github.tonivade.purefun.typeclasses.Monoid;
import com.github.tonivade.purefun.typeclasses.Traverse;

public interface ConstInstances {

  static <T, A> Eq<Kind<Kind<Const_, T>, A>> eq(Eq<T> eq) {
    return (a, b) -> eq.eqv(a.fix(ConstOf::narrowK).get(), a.fix(ConstOf::narrowK).get());
  }

  @SuppressWarnings("unchecked")
  static <T> Functor<Kind<Const_, T>> functor() {
    return ConstFunctor.INSTANCE;
  }

  static <T> Applicative<Kind<Const_, T>> applicative(Monoid<T> monoid) {
    return ConstApplicative.instance(monoid);
  }

  @SuppressWarnings("unchecked")
  static <T> Foldable<Kind<Const_, T>> foldable() {
    return ConstFoldable.INSTANCE;
  }

  @SuppressWarnings("unchecked")
  static <T> Traverse<Kind<Const_, T>> traverse() {
    return ConstTraverse.INSTANCE;
  }

  @SuppressWarnings("unchecked")
  static <T> Contravariant<Kind<Const_, T>> contravariant() {
    return ConstContravariant.INSTANCE;
  }
}

interface ConstFunctor<T> extends Functor<Kind<Const_, T>> {

  @SuppressWarnings("rawtypes")
  ConstFunctor INSTANCE = new ConstFunctor() {};

  @Override
  default <A, B> Kind<Kind<Const_, T>, B> map(Kind<Kind<Const_, T>, A> value, Function1<? super A, ? extends B> map) {
    return value.fix(ConstOf::narrowK).<B>retag();
  }
}

interface ConstApplicative<T> extends Applicative<Kind<Const_, T>> {

  static <T> ConstApplicative<T> instance(Monoid<T> monoid) {
    return () -> monoid;
  }

  Monoid<T> monoid();

  @Override
  default <A> Const<T, A> pure(A value) {
    return Const.<T, A>of(monoid().zero());
  }

  @Override
  default <A, B> Const<T, B> ap(
      Kind<Kind<Const_, T>, A> value, Kind<Kind<Const_, T>, Function1<? super A, ? extends B>> apply) {
    return Const.<T, B>of(monoid().combine(
            apply.fix(ConstOf::narrowK).<B>retag().get(),
            value.fix(ConstOf::narrowK).<B>retag().get()));
  }
}

interface ConstContravariant<T> extends Contravariant<Kind<Const_, T>> {

  @SuppressWarnings("rawtypes")
  ConstContravariant INSTANCE = new ConstContravariant() {};

  @Override
  default <A, B> Const<T, B> contramap(Kind<Kind<Const_, T>, A> value, Function1<? super B, ? extends A> map) {
    return value.fix(ConstOf::narrowK).<B>retag();
  }
}

interface ConstFoldable<T> extends Foldable<Kind<Const_, T>> {

  @SuppressWarnings("rawtypes")
  ConstFoldable INSTANCE = new ConstFoldable() {};

  @Override
  default <A, B> B foldLeft(Kind<Kind<Const_, T>, A> value, B initial, Function2<B, A, B> mapper) {
    return initial;
  }

  @Override
  default <A, B> Eval<B> foldRight(
      Kind<Kind<Const_, T>, A> value, Eval<B> initial, Function2<A, Eval<B>, Eval<B>> mapper) {
    return initial;
  }
}

interface ConstTraverse<T> extends Traverse<Kind<Const_, T>>, ConstFoldable<T> {

  @SuppressWarnings("rawtypes")
  ConstTraverse INSTANCE = new ConstTraverse() {};

  @Override
  default <G extends Witness, A, B> Kind<G, Kind<Kind<Const_, T>, B>> traverse(
      Applicative<G> applicative, Kind<Kind<Const_, T>, A> value, Function1<? super A, ? extends Kind<G, ? extends B>> mapper) {
    return applicative.pure(value.fix(ConstOf::narrowK).<B>retag());
  }
}
