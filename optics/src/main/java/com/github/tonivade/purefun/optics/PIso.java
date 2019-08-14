/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.optics;

import static java.util.Objects.requireNonNull;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.type.Either;

public final class PIso<S, T, A, B> {

  private final Function1<S, A> get;
  private final Function1<B, T> reverseGet;

  protected PIso(Function1<S, A> get, Function1<B, T> reverseGet) {
    this.get = requireNonNull(get);
    this.reverseGet = requireNonNull(reverseGet);
  }

  public static <S, T, A, B> PIso<S, T, A, B> of(Function1<S, A> get, Function1<B, T> reverseGet) {
    return new PIso<>(get, reverseGet);
  }

  public static <S, T> PIso<S, T, S, T> identity() {
    return new PIso<>(Function1.identity(), Function1.identity());
  }

  public PIso<B, A, T, S> reverse() {
    return new PIso<>(reverseGet, get);
  }

  public A get(S target) {
    return get.apply(target);
  }

  public T set(B value) {
    return reverseGet.apply(value);
  }

  public T modify(S target, Function1<A, B> mapper) {
    return lift(mapper).apply(target);
  }

  public Function1<S, T> lift(Function1<A, B> mapper) {
    return mapper.compose(get).andThen(reverseGet)::apply;
  }

  public PLens<S, T, A, B> asLens() {
    return PLens.of(this.get, (target, value) -> this.set(value));
  }

  public PPrism<S, T, A, B> asPrism() {
    return PPrism.of(this.get.andThen(Either::right), reverseGet);
  }

  public POptional<S, T, A, B> asOptional() {
    return POptional.of((target, value) -> this.set(value), this.get.andThen(Either::right));
  }

  public <C, D> PIso<S, T, C, D> compose(PIso<A, B, C, D> other) {
    return new PIso<>(
        this.get.andThen(other.get),
        this.reverseGet.compose(other.reverseGet));
  }

  public <C, D> PLens<S, T, C, D> compose(PLens<A, B, C, D> other) {
    return asLens().compose(other);
  }

  public <C, D> PPrism<S, T, C, D> compose(PPrism<A, B, C, D> other) {
    return asPrism().compose(other);
  }

  public <C, D> POptional<S, T, C, D> compose(POptional<A, B, C, D> other) {
    return asOptional().compose(other);
  }
}
