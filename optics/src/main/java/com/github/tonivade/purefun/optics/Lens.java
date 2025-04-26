/*
 * Copyright (c) 2018-2025, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.optics;

import static com.github.tonivade.purefun.core.Precondition.checkNonNull;

import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Function2;
import com.github.tonivade.purefun.core.Operator1;

public final class Lens<S, A> {

  private final PLens<S, S, A, A> delegate;

  Lens(PLens<S, S, A, A> delegate) {
    this.delegate = checkNonNull(delegate);
  }

  public static <S, A> Lens<S, A> of(Function1<S, A> getter, Function2<S, A, S> setter) {
    return new Lens<>(PLens.of(getter, setter));
  }

  public A get(S target) {
    return delegate.get(target);
  }

  public S set(S target, A value) {
    return delegate.set(target, value);
  }

  public Function1<A, S> set(S target) {
    return delegate.set(target);
  }

  public Operator1<S> modify(Operator1<A> mapper) {
    return delegate.modify(mapper)::apply;
  }

  public Operator1<S> modify(A newValue) {
    return delegate.modify(newValue)::apply;
  }

  public Optional<S, A> asOptional() {
    return new Optional<>(delegate.asOptional());
  }

  public <B> Lens<S, B> compose(Lens<A, B> other) {
    return new Lens<>(delegate.compose(other.delegate));
  }

  public <B> Optional<S, B> compose(Optional<A, B> other) {
    return asOptional().compose(other);
  }

  public <B> Lens<S, B> compose(Iso<A, B> other) {
    return compose(other.asLens());
  }

  public <B> Optional<S, B> compose(Prism<A, B> other) {
    return asOptional().compose(other.asOptional());
  }
}
