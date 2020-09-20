/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import static com.github.tonivade.purefun.type.IdOf.toId;

import com.github.tonivade.purefun.Eq;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Function2;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Witness;
import com.github.tonivade.purefun.type.Eval;
import com.github.tonivade.purefun.type.Id;
import com.github.tonivade.purefun.type.IdOf;
import com.github.tonivade.purefun.type.Id_;
import com.github.tonivade.purefun.typeclasses.Applicative;
import com.github.tonivade.purefun.typeclasses.Comonad;
import com.github.tonivade.purefun.typeclasses.Foldable;
import com.github.tonivade.purefun.typeclasses.Functor;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.Traverse;

public interface IdInstances {

  static <T> Eq<Kind<Id_, T>> eq(Eq<T> idEq) {
    return (a, b) -> idEq.eqv(IdOf.narrowK(a).get(), IdOf.narrowK(b).get());
  }

  static Functor<Id_> functor() {
    return IdFunctor.INSTANCE;
  }

  static Applicative<Id_> applicative() {
    return IdApplicative.INSTANCE;
  }

  static Monad<Id_> monad() {
    return IdMonad.INSTANCE;
  }

  static Comonad<Id_> comonad() {
    return IdComonad.INSTANCE;
  }

  static Foldable<Id_> foldable() {
    return IdFoldable.INSTANCE;
  }

  static Traverse<Id_> traverse() {
    return IdTraverse.INSTANCE;
  }
}

interface IdFunctor extends Functor<Id_> {

  IdFunctor INSTANCE = new IdFunctor() {};

  @Override
  default <T, R> Kind<Id_, R> map(Kind<Id_, T> value, Function1<? super T, ? extends R> map) {
    return IdOf.narrowK(value).map(map);
  }
}

interface IdPure extends Applicative<Id_> {

  @Override
  default <T> Kind<Id_, T> pure(T value) {
    return Id.of(value);
  }
}

interface IdApplicative extends IdPure {

  IdApplicative INSTANCE = new IdApplicative() {};

  @Override
  default <T, R> Kind<Id_, R> ap(Kind<Id_, T> value, Kind<Id_, Function1<T, R>> apply) {
    return IdOf.narrowK(value).flatMap(t -> IdOf.narrowK(apply).map(f -> f.apply(t)));
  }
}

interface IdMonad extends IdPure, Monad<Id_> {

  IdMonad INSTANCE = new IdMonad() {};

  @Override
  default <T, R> Kind<Id_, R> flatMap(Kind<Id_, T> value, Function1<T, ? extends Kind<Id_, R>> map) {
    return IdOf.narrowK(value).flatMap(map.andThen(IdOf::narrowK));
  }
}

interface IdComonad extends IdFunctor, Comonad<Id_> {

  IdComonad INSTANCE = new IdComonad() {};

  @Override
  default <A, B> Kind<Id_, B> coflatMap(Kind<Id_, A> value, Function1<? super Kind<Id_, ? extends A>, ? extends B> map) {
    return Id.of(map.apply(value));
  }

  @Override
  default <A> A extract(Kind<Id_, A> value) {
    return IdOf.narrowK(value).get();
  }
}

interface IdFoldable extends Foldable<Id_> {

  IdFoldable INSTANCE = new IdFoldable() {};

  @Override
  default <A, B> B foldLeft(Kind<Id_, A> value, B initial, Function2<B, A, B> mapper) {
    return mapper.apply(initial, IdOf.narrowK(value).get());
  }

  @Override
  default <A, B> Eval<B> foldRight(Kind<Id_, A> value, Eval<B> initial, Function2<A, Eval<B>, Eval<B>> mapper) {
    return mapper.apply(IdOf.narrowK(value).get(), initial);
  }
}

interface IdTraverse extends Traverse<Id_>, IdFoldable {

  IdTraverse INSTANCE = new IdTraverse() {};

  @Override
  default <G extends Witness, T, R> Kind<G, Kind<Id_, R>> traverse(
      Applicative<G> applicative, Kind<Id_, T> value,
      Function1<? super T, ? extends Kind<G, ? extends R>> mapper) {
    Kind<G, ? extends R> apply = mapper.apply(value.fix(toId()).get());
    return applicative.map(apply, a -> Id.of(a));
  }
}
