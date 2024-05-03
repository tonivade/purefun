/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import com.github.tonivade.purefun.Kind;

import com.github.tonivade.purefun.core.Eq;
import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Function2;
import com.github.tonivade.purefun.type.Const;
import com.github.tonivade.purefun.type.ConstOf;
import com.github.tonivade.purefun.type.Eval;
import com.github.tonivade.purefun.type.EvalOf;
import com.github.tonivade.purefun.typeclasses.Applicative;
import com.github.tonivade.purefun.typeclasses.Contravariant;
import com.github.tonivade.purefun.typeclasses.Foldable;
import com.github.tonivade.purefun.typeclasses.Functor;
import com.github.tonivade.purefun.typeclasses.Monoid;
import com.github.tonivade.purefun.typeclasses.Traverse;

  @SuppressWarnings("unchecked")
public interface ConstInstances {

  static <T, A> Eq<Kind<Kind<Const<?, ?>, T>, A>> eq(Eq<T> eq) {
    return (a, b) -> eq.eqv(a.fix(ConstOf::narrowK).value(), a.fix(ConstOf::narrowK).value());
  }

  static <T> Functor<Kind<Const<?, ?>, T>> functor() {
    return ConstFunctor.INSTANCE;
  }

  static <T> Applicative<Kind<Const<?, ?>, T>> applicative(Monoid<T> monoid) {
    return ConstApplicative.instance(monoid);
  }

  static <T> Foldable<Kind<Const<?, ?>, T>> foldable() {
    return ConstFoldable.INSTANCE;
  }

  static <T> Traverse<Kind<Const<?, ?>, T>> traverse() {
    return ConstTraverse.INSTANCE;
  }

  static <T> Contravariant<Kind<Const<?, ?>, T>> contravariant() {
    return ConstContravariant.INSTANCE;
  }
}

interface ConstFunctor<T> extends Functor<Kind<Const<?, ?>, T>> {

  @SuppressWarnings("rawtypes")
  ConstFunctor INSTANCE = new ConstFunctor() {};

  @Override
  default <A, B> Kind<Kind<Const<?, ?>, T>, B> map(Kind<Kind<Const<?, ?>, T>, ? extends A> value, Function1<? super A, ? extends B> map) {
    return value.fix(ConstOf::narrowK).retag();
  }
}

interface ConstApplicative<T> extends Applicative<Kind<Const<?, ?>, T>> {

  static <T> ConstApplicative<T> instance(Monoid<T> monoid) {
    return () -> monoid;
  }

  Monoid<T> monoid();

  @Override
  default <A> Const<T, A> pure(A value) {
    return Const.of(monoid().zero());
  }

  @Override
  default <A, B> Const<T, B> ap(
      Kind<Kind<Const<?, ?>, T>, ? extends A> value,
      Kind<Kind<Const<?, ?>, T>, ? extends Function1<? super A, ? extends B>> apply) {
    return Const.of(monoid().combine(
            apply.fix(ConstOf::narrowK).<B>retag().value(),
            value.fix(ConstOf::narrowK).<B>retag().value()));
  }
}

interface ConstContravariant<T> extends Contravariant<Kind<Const<?, ?>, T>> {

  @SuppressWarnings("rawtypes")
  ConstContravariant INSTANCE = new ConstContravariant() {};

  @Override
  default <A, B> Const<T, B> contramap(Kind<Kind<Const<?, ?>, T>, ? extends A> value, Function1<? super B, ? extends A> map) {
    return value.fix(ConstOf::narrowK).retag();
  }
}

interface ConstFoldable<T> extends Foldable<Kind<Const<?, ?>, T>> {

  @SuppressWarnings("rawtypes")
  ConstFoldable INSTANCE = new ConstFoldable() {};

  @Override
  default <A, B> B foldLeft(Kind<Kind<Const<?, ?>, T>, ? extends A> value, B initial,
      Function2<? super B, ? super A, ? extends B> mapper) {
    return initial;
  }

  @Override
  default <A, B> Eval<B> foldRight(
      Kind<Kind<Const<?, ?>, T>, ? extends A> value, Eval<? extends B> initial,
      Function2<? super A, ? super Eval<? extends B>, ? extends Eval<? extends B>> mapper) {
    return EvalOf.narrowK(initial);
  }
}

interface ConstTraverse<T> extends Traverse<Kind<Const<?, ?>, T>>, ConstFoldable<T> {

  @SuppressWarnings("rawtypes")
  ConstTraverse INSTANCE = new ConstTraverse() {};

  @Override
  default <G, A, B> Kind<G, Kind<Kind<Const<?, ?>, T>, B>> traverse(
      Applicative<G> applicative, Kind<Kind<Const<?, ?>, T>, A> value, Function1<? super A, ? extends Kind<G, ? extends B>> mapper) {
    return applicative.pure(value.fix(ConstOf::narrowK).retag());
  }
}
