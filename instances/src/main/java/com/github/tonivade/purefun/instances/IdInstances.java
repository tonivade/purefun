/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import com.github.tonivade.purefun.Eq;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Function2;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Instance;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.type.Eval;
import com.github.tonivade.purefun.type.Id;
import com.github.tonivade.purefun.typeclasses.Applicative;
import com.github.tonivade.purefun.typeclasses.Comonad;
import com.github.tonivade.purefun.typeclasses.Foldable;
import com.github.tonivade.purefun.typeclasses.Functor;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.Traverse;

public interface IdInstances {

  static <T> Eq<Higher1<Id.µ, T>> eq(Eq<T> idEq) {
    return (a, b) -> idEq.eqv(Id.narrowK(a).get(), Id.narrowK(b).get());
  }

  static Functor<Id.µ> functor() {
    return IdFunctor.INSTANCE;
  }

  static Applicative<Id.µ> applicative() {
    return IdApplicative.INSTANCE;
  }

  static Monad<Id.µ> monad() {
    return IdMonad.INSTANCE;
  }

  static Comonad<Id.µ> comonad() {
    return IdComonad.INSTANCE;
  }

  static Foldable<Id.µ> foldable() {
    return IdFoldable.INSTANCE;
  }

  static Traverse<Id.µ> traverse() {
    return IdTraverse.INSTANCE;
  }
}

@Instance
interface IdFunctor extends Functor<Id.µ> {

  IdFunctor INSTANCE = new IdFunctor() { };

  @Override
  default <T, R> Higher1<Id.µ, R> map(Higher1<Id.µ, T> value, Function1<T, R> map) {
    return Id.narrowK(value).map(map).kind1();
  }
}

interface IdPure extends Applicative<Id.µ> {

  @Override
  default <T> Higher1<Id.µ, T> pure(T value) {
    return Id.of(value).kind1();
  }
}

@Instance
interface IdApplicative extends IdPure {

  IdApplicative INSTANCE = new IdApplicative() { };

  @Override
  default <T, R> Higher1<Id.µ, R> ap(Higher1<Id.µ, T> value, Higher1<Id.µ, Function1<T, R>> apply) {
    return Id.narrowK(value).flatMap(t -> Id.narrowK(apply).map(f -> f.apply(t))).kind1();
  }
}

@Instance
interface IdMonad extends IdPure, Monad<Id.µ> {

  IdMonad INSTANCE = new IdMonad() { };

  @Override
  default <T, R> Higher1<Id.µ, R> flatMap(Higher1<Id.µ, T> value, Function1<T, ? extends Higher1<Id.µ, R>> map) {
    return Id.narrowK(value).flatMap(map.andThen(Id::narrowK)).kind1();
  }
}

@Instance
interface IdComonad extends IdFunctor, Comonad<Id.µ> {

  IdComonad INSTANCE = new IdComonad() { };

  @Override
  default <A, B> Higher1<Id.µ, B> coflatMap(Higher1<Id.µ, A> value, Function1<Higher1<Id.µ, A>, B> map) {
    return Id.of(map.apply(value)).kind1();
  }

  @Override
  default <A> A extract(Higher1<Id.µ, A> value) {
    return Id.narrowK(value).get();
  }
}

@Instance
interface IdFoldable extends Foldable<Id.µ> {

  IdFoldable INSTANCE = new IdFoldable() { };

  @Override
  default <A, B> B foldLeft(Higher1<Id.µ, A> value, B initial, Function2<B, A, B> mapper) {
    return mapper.apply(initial, Id.narrowK(value).get());
  }

  @Override
  default <A, B> Eval<B> foldRight(Higher1<Id.µ, A> value, Eval<B> initial, Function2<A, Eval<B>, Eval<B>> mapper) {
    return mapper.apply(Id.narrowK(value).get(), initial);
  }
}

@Instance
interface IdTraverse extends Traverse<Id.µ>, IdFoldable {

  IdTraverse INSTANCE = new IdTraverse() { };

  @Override
  default <G extends Kind, T, R> Higher1<G, Higher1<Id.µ, R>> traverse(
      Applicative<G> applicative, Higher1<Id.µ, T> value,
      Function1<T, ? extends Higher1<G, R>> mapper) {
    return applicative.map(mapper.apply(Id.narrowK(value).get()), a -> Id.of(a).kind1());
  }
}
