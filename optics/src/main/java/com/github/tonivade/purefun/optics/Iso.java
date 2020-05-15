/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.optics;

import static com.github.tonivade.purefun.Precondition.checkNonNull;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Operator1;

public final class Iso<S, A> {

  private final PIso<S, S, A, A> delegate;

  protected Iso(PIso<S, S, A, A> delegate) {
    this.delegate = checkNonNull(delegate);
  }

  public static <S, A> Iso<S, A> of(Function1<S, A> get, Function1<A, S> reverseGet) {
    return new Iso<>(PIso.of(get, reverseGet));
  }

  public static <S> Iso<S, S> identity() {
    return new Iso<>(PIso.identity());
  }

  public Iso<A, S> reverse() {
    return new Iso<>(delegate.reverse());
  }

  public A get(S target) {
    return delegate.get(target);
  }

  public S set(A value) {
    return delegate.set(value);
  }

  public S modify(S target, Operator1<A> mapper) {
    return delegate.modify(target, mapper);
  }

  public Operator1<S> lift(Operator1<A> mapper) {
    return delegate.lift(mapper)::apply;
  }

  public Lens<S, A> asLens() {
    return new Lens<>(delegate.asLens());
  }

  public Prism<S, A> asPrism() {
    return new Prism<>(delegate.asPrism());
  }

  public Optional<S, A> asOptional() {
    return new Optional<>(delegate.asOptional());
  }

  public <B> Iso<S, B> compose(Iso<A, B> other) {
    return new Iso<>(delegate.compose(other.delegate));
  }

  public <B> Lens<S, B> compose(Lens<A, B> other) {
    return asLens().compose(other);
  }

  public <B> Prism<S, B> compose(Prism<A, B> other) {
    return asPrism().compose(other);
  }

  public <B> Optional<S, B> compose(Optional<A, B> other) {
    return asOptional().compose(other);
  }
}
