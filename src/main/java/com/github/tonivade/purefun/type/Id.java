/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.type;

import static com.github.tonivade.purefun.Function1.identity;
import static com.github.tonivade.purefun.typeclasses.Eq.comparing;
import static java.util.Objects.requireNonNull;

import java.util.Objects;

import com.github.tonivade.purefun.FlatMap1;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Function2;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Holder;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.typeclasses.Applicative;
import com.github.tonivade.purefun.typeclasses.Eq;
import com.github.tonivade.purefun.typeclasses.Equal;
import com.github.tonivade.purefun.typeclasses.Foldable;
import com.github.tonivade.purefun.typeclasses.Functor;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.Traverse;

public class Id<T> implements Holder<T>, FlatMap1<Id.µ, T> {

  public static final class µ implements Kind { }

  private final T value;

  private Id(T value) {
    this.value = requireNonNull(value);
  }

  @Override
  public <R> Id<R> map(Function1<T, R> map) {
    return map.andThen(Id::of).apply(value);
  }

  @Override
  public <R> Id<R> flatMap(Function1<T, ? extends Higher1<Id.µ, R>> map) {
    return map.andThen(Id::narrowK).apply(value);
  }

  @Override
  public T get() {
    return value;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <V> Id<V> flatten() {
    try {
      return ((Id<Id<V>>) this).flatMap(identity());
    } catch (ClassCastException e) {
      throw new UnsupportedOperationException("cannot be flattened");
    }
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }

  @Override
  public boolean equals(Object obj) {
    return Equal.of(this).append(comparing(Id::get)).applyTo(obj);
  }

  @Override
  public String toString() {
    return "Id(" + value + ")";
  }

  public static <T> Id<T> of(T value) {
    return new Id<>(value);
  }

  public static <T> Id<T> narrowK(Higher1<Id.µ, T> hkt) {
    return (Id<T>) hkt;
  }

  public static <T> Eq<Higher1<Id.µ, T>> eq(Eq<T> idEq) {
    return (a, b) -> idEq.eqv(narrowK(a).get(), narrowK(b).get());
  }

  public static Functor<Id.µ> functor() {
    return new IdFunctor() {};
  }

  public static Applicative<Id.µ> applicative() {
    return new IdApplicative() {};
  }

  public static Monad<Id.µ> monad() {
    return new IdMonad() {};
  }

  public static Foldable<Id.µ> foldable() {
    return new IdFoldable() {};
  }

  public static Traverse<Id.µ> traverse() {
    return new IdTraverse() {};
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

interface IdApply extends Applicative<Id.µ> {

  @Override
  default <T, R> Id<R> ap(Higher1<Id.µ, T> value, Higher1<Id.µ, Function1<T, R>> apply) {
    return Id.narrowK(value).flatMap(t -> Id.narrowK(apply).map(f -> f.apply(t)));
  }
}

interface IdApplicative extends IdPure, IdApply {}

interface IdMonad extends IdPure, Monad<Id.µ> {

  @Override
  default <T, R> Id<R> flatMap(Higher1<Id.µ, T> value, Function1<T, ? extends Higher1<Id.µ, R>> map) {
    return Id.narrowK(value).flatMap(map);
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