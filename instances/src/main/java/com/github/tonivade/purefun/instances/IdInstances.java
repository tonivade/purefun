/*
 * Copyright (c) 2018-2025, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import com.github.tonivade.purefun.Kind;

import com.github.tonivade.purefun.core.Eq;
import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Function2;
import com.github.tonivade.purefun.type.Eval;
import com.github.tonivade.purefun.type.EvalOf;
import com.github.tonivade.purefun.type.Id;
import com.github.tonivade.purefun.type.IdOf;
import com.github.tonivade.purefun.typeclasses.Applicative;
import com.github.tonivade.purefun.typeclasses.Comonad;
import com.github.tonivade.purefun.typeclasses.Foldable;
import com.github.tonivade.purefun.typeclasses.Functor;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.Traverse;

public interface IdInstances {

  static <T> Eq<Kind<Id<?>, T>> eq(Eq<T> idEq) {
    return (a, b) -> idEq.eqv(IdOf.toId(a).value(), IdOf.toId(b).value());
  }

  static Functor<Id<?>> functor() {
    return IdFunctor.INSTANCE;
  }

  static Applicative<Id<?>> applicative() {
    return IdApplicative.INSTANCE;
  }

  static Monad<Id<?>> monad() {
    return IdMonad.INSTANCE;
  }

  static Comonad<Id<?>> comonad() {
    return IdComonad.INSTANCE;
  }

  static Foldable<Id<?>> foldable() {
    return IdFoldable.INSTANCE;
  }

  static Traverse<Id<?>> traverse() {
    return IdTraverse.INSTANCE;
  }
}

interface IdFunctor extends Functor<Id<?>> {

  IdFunctor INSTANCE = new IdFunctor() {};

  @Override
  default <T, R> Kind<Id<?>, R> map(Kind<Id<?>, ? extends T> value, Function1<? super T, ? extends R> map) {
    return IdOf.toId(value).map(map);
  }
}

interface IdPure extends Applicative<Id<?>> {

  @Override
  default <T> Kind<Id<?>, T> pure(T value) {
    return Id.of(value);
  }
}

interface IdApplicative extends IdPure {

  IdApplicative INSTANCE = new IdApplicative() {};

  @Override
  default <T, R> Kind<Id<?>, R> ap(Kind<Id<?>, ? extends T> value,
      Kind<Id<?>, ? extends Function1<? super T, ? extends R>> apply) {
    return IdOf.toId(value).flatMap(t -> IdOf.toId(apply).map(f -> f.apply(t)));
  }
}

interface IdMonad extends IdPure, Monad<Id<?>> {

  IdMonad INSTANCE = new IdMonad() {};

  @Override
  default <T, R> Kind<Id<?>, R> flatMap(Kind<Id<?>, ? extends T> value, Function1<? super T, ? extends Kind<Id<?>, ? extends R>> map) {
    return IdOf.toId(value).flatMap(map.andThen(IdOf::toId));
  }
}

interface IdComonad extends IdFunctor, Comonad<Id<?>> {

  IdComonad INSTANCE = new IdComonad() {};

  @Override
  default <A, B> Kind<Id<?>, B> coflatMap(Kind<Id<?>, ? extends A> value, Function1<? super Kind<Id<?>, ? extends A>, ? extends B> map) {
    return Id.of(map.apply(value));
  }

  @Override
  default <A> A extract(Kind<Id<?>, ? extends A> value) {
    return IdOf.toId(value).value();
  }
}

interface IdFoldable extends Foldable<Id<?>> {

  IdFoldable INSTANCE = new IdFoldable() {};

  @Override
  default <A, B> B foldLeft(Kind<Id<?>, ? extends A> value, B initial, Function2<? super B, ? super A, ? extends B> mapper) {
    return mapper.apply(initial, value.fix(IdOf::toId).value());
  }

  @Override
  default <A, B> Eval<B> foldRight(Kind<Id<?>, ? extends A> value, Eval<? extends B> initial, Function2<? super A, ? super Eval<? extends B>, ? extends Eval<? extends B>> mapper) {
    return EvalOf.toEval(mapper.apply(value.fix(IdOf::toId).value(), initial));
  }
}

interface IdTraverse extends Traverse<Id<?>>, IdFoldable {

  IdTraverse INSTANCE = new IdTraverse() {};

  @Override
  default <G extends Kind<G, ?>, T, R> Kind<G, Kind<Id<?>, R>> traverse(
      Applicative<G> applicative, Kind<Id<?>, T> value,
      Function1<? super T, ? extends Kind<G, ? extends R>> mapper) {
    Kind<G, ? extends R> apply = mapper.apply(value.fix(IdOf::toId).value());
    return applicative.map(apply, Id::of);
  }
}
