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
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Holder;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.typeclasses.Applicative;
import com.github.tonivade.purefun.typeclasses.Eq;
import com.github.tonivade.purefun.typeclasses.Equal;
import com.github.tonivade.purefun.typeclasses.Functor;
import com.github.tonivade.purefun.typeclasses.Monad;

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
    return new Functor<Id.µ>() {

      @Override
      public <T, R> Id<R> map(Higher1<Id.µ, T> value, Function1<T, R> map) {
        return narrowK(value).map(map);
      }
    };
  }

  public static Applicative<Id.µ> applicative() {
    return new Applicative<Id.µ>() {

      @Override
      public <T> Id<T> pure(T value) {
        return of(value);
      }

      @Override
      public <T, R> Id<R> ap(Higher1<Id.µ, T> value, Higher1<Id.µ, Function1<T, R>> apply) {
        return narrowK(value).flatMap(t -> narrowK(apply).map(f -> f.apply(t)));
      }
    };
  }

  public static Monad<Id.µ> monad() {
    return new Monad<Id.µ>() {

      @Override
      public <T> Id<T> pure(T value) {
        return of(value);
      }

      @Override
      public <T, R> Id<R> flatMap(Higher1<Id.µ, T> value, Function1<T, ? extends Higher1<Id.µ, R>> map) {
        return narrowK(value).flatMap(map);
      }
    };
  }
}
