/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Function2;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.type.Eval;
import com.github.tonivade.purefun.type.Id;
import com.github.tonivade.purefun.typeclasses.Applicative;
import com.github.tonivade.purefun.typeclasses.Comonad;
import com.github.tonivade.purefun.typeclasses.Defer;
import com.github.tonivade.purefun.typeclasses.Eq;
import com.github.tonivade.purefun.typeclasses.Foldable;
import com.github.tonivade.purefun.typeclasses.Functor;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.Traverse;

public interface IdInstances {

  static <T> Eq<Higher1<Id.µ, T>> eq(Eq<T> idEq) {
    return (a, b) -> idEq.eqv(Id.narrowK(a).get(), Id.narrowK(b).get());
  }

  static Functor<Id.µ> functor() {
    return new IdFunctor() {};
  }

  static Applicative<Id.µ> applicative() {
    return new IdApplicative() {};
  }

  static Monad<Id.µ> monad() {
    return new IdMonad() {};
  }

  static Comonad<Id.µ> comonad() {
    return new IdComonad() {};
  }

  static Foldable<Id.µ> foldable() {
    return new IdFoldable() {};
  }

  static Traverse<Id.µ> traverse() {
    return new IdTraverse() {};
  }

  static Defer<Id.µ> defer() {
    return new IdDefer() {};
  }
}

interface IdFunctor extends Functor<Id.µ> {

  @Override
  default <T, R> Id<R> map(Higher1<Id.µ, T> value, Function1<T, R> map) {
    return Id.narrowK(value).map(map);
  }
}

interface IdPure extends Applicative<Id.µ> {

  @Override
  default <T> Id<T> pure(T value) {
    return Id.of(value);
  }
}

interface IdApplicative extends IdPure {

  @Override
  default <T, R> Id<R> ap(Higher1<Id.µ, T> value, Higher1<Id.µ, Function1<T, R>> apply) {
    return Id.narrowK(value).flatMap(t -> Id.narrowK(apply).map(f -> f.apply(t)));
  }
}

interface IdMonad extends IdPure, Monad<Id.µ> {

  @Override
  default <T, R> Id<R> flatMap(Higher1<Id.µ, T> value, Function1<T, ? extends Higher1<Id.µ, R>> map) {
    return Id.narrowK(value).flatMap(map);
  }
}

interface IdComonad extends IdFunctor, Comonad<Id.µ> {

  @Override
  default <A, B> Id<B> coflatMap(Higher1<Id.µ, A> value, Function1<Higher1<Id.µ, A>, B> map) {
    return Id.of(map.apply(value));
  }

  @Override
  default <A> A extract(Higher1<Id.µ, A> value) {
    return Id.narrowK(value).get();
  }
}

interface IdFoldable extends Foldable<Id.µ> {

  @Override
  default <A, B> B foldLeft(Higher1<Id.µ, A> value, B initial, Function2<B, A, B> mapper) {
    return mapper.apply(initial, Id.narrowK(value).get());
  }

  @Override
  default <A, B> Eval<B> foldRight(Higher1<Id.µ, A> value, Eval<B> initial, Function2<A, Eval<B>, Eval<B>> mapper) {
    return mapper.apply(Id.narrowK(value).get(), initial);
  }
}

interface IdTraverse extends Traverse<Id.µ>, IdFoldable {

  @Override
  default <G extends Kind, T, R> Higher1<G, Higher1<Id.µ, R>> traverse(
      Applicative<G> applicative, Higher1<Id.µ, T> value,
      Function1<T, ? extends Higher1<G, R>> mapper) {
    return applicative.map(mapper.apply(Id.narrowK(value).get()), Id::of);
  }
}

interface IdDefer extends Defer<Id.µ> {

  @Override
  default <A> Id<A> defer(Producer<Higher1<Id.µ, A>> defer) {
    return defer.andThen(Id::narrowK).get();
  }
}
