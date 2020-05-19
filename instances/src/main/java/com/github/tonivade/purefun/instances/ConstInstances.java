/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import com.github.tonivade.purefun.Eq;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Function2;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Higher2;
import com.github.tonivade.purefun.Kind;
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

  static <T, A> Eq<Higher1<Higher1<Const_, T>, A>> eq(Eq<T> eq) {
    return (a, b) -> eq.eqv(a.fix1(ConstOf::narrowK).get(), a.fix1(ConstOf::narrowK).get());
  }

  @SuppressWarnings("unchecked")
  static <T> Functor<Higher1<Const_, T>> functor() {
    return ConstFunctor.INSTANCE;
  }

  static <T> Applicative<Higher1<Const_, T>> applicative(Monoid<T> monoid) {
    return ConstApplicative.instance(monoid);
  }

  @SuppressWarnings("unchecked")
  static <T> Foldable<Higher1<Const_, T>> foldable() {
    return ConstFoldable.INSTANCE;
  }

  @SuppressWarnings("unchecked")
  static <T> Traverse<Higher1<Const_, T>> traverse() {
    return ConstTraverse.INSTANCE;
  }

  @SuppressWarnings("unchecked")
  static <T> Contravariant<Higher1<Const_, T>> contravariant() {
    return ConstContravariant.INSTANCE;
  }
}

interface ConstFunctor<T> extends Functor<Higher1<Const_, T>> {

  @SuppressWarnings("rawtypes")
  ConstFunctor INSTANCE = new ConstFunctor() {};

  @Override
  default <A, B> Higher1<Higher1<Const_, T>, B> map(Higher1<Higher1<Const_, T>, A> value, Function1<A, B> map) {
    return value.fix1(ConstOf::narrowK).<B>retag();
  }
}

interface ConstApplicative<T> extends Applicative<Higher1<Const_, T>> {

  static <T> ConstApplicative<T> instance(Monoid<T> monoid) {
    return () -> monoid;
  }

  Monoid<T> monoid();

  @Override
  default <A> Higher2<Const_, T, A> pure(A value) {
    return Const.<T, A>of(monoid().zero());
  }

  @Override
  default <A, B> Higher2<Const_, T, B> ap(
      Higher1<Higher1<Const_, T>, A> value, Higher1<Higher1<Const_, T>, Function1<A, B>> apply) {
    return Const.<T, B>of(monoid().combine(
            apply.fix1(ConstOf::narrowK).<B>retag().get(),
            value.fix1(ConstOf::narrowK).<B>retag().get()));
  }
}

interface ConstContravariant<T> extends Contravariant<Higher1<Const_, T>> {

  @SuppressWarnings("rawtypes")
  ConstContravariant INSTANCE = new ConstContravariant() {};

  @Override
  default <A, B> Higher2<Const_, T, B> contramap(Higher1<Higher1<Const_, T>, A> value, Function1<B, A> map) {
    return value.fix1(ConstOf::narrowK).<B>retag();
  }
}

interface ConstFoldable<T> extends Foldable<Higher1<Const_, T>> {

  @SuppressWarnings("rawtypes")
  ConstFoldable INSTANCE = new ConstFoldable() {};

  @Override
  default <A, B> B foldLeft(Higher1<Higher1<Const_, T>, A> value, B initial, Function2<B, A, B> mapper) {
    return initial;
  }

  @Override
  default <A, B> Eval<B> foldRight(
      Higher1<Higher1<Const_, T>, A> value, Eval<B> initial, Function2<A, Eval<B>, Eval<B>> mapper) {
    return initial;
  }
}

interface ConstTraverse<T> extends Traverse<Higher1<Const_, T>>, ConstFoldable<T> {

  @SuppressWarnings("rawtypes")
  ConstTraverse INSTANCE = new ConstTraverse() {};

  @Override
  default <G extends Kind, A, B> Higher1<G, Higher1<Higher1<Const_, T>, B>> traverse(
      Applicative<G> applicative, Higher1<Higher1<Const_, T>, A> value, Function1<A, ? extends Higher1<G, B>> mapper) {
    return applicative.pure(value.fix1(ConstOf::narrowK).<B>retag());
  }
}
