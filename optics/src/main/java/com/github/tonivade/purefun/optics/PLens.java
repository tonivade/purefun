/*
 * Copyright (c) 2018-2021, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.optics;

import static com.github.tonivade.purefun.Function1.cons;
import static com.github.tonivade.purefun.Precondition.checkNonNull;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Function2;
import com.github.tonivade.purefun.type.Either;

public final class PLens<S, T, A, B> {

  private final Function1<S, A> getter;
  private final Function1<S, Function1<B, T>> setter;

  protected PLens(Function1<S, A> getter, Function1<S, Function1<B, T>> setter) {
    this.getter = checkNonNull(getter);
    this.setter = checkNonNull(setter);
  }

  public static <S, T, A, B> PLens<S, T, A, B> of(Function1<S, A> getter, Function2<S, B, T> setter) {
    return new PLens<>(getter, setter.curried());
  }

  public A get(S target) {
    return getter.apply(target);
  }

  public T set(S target, B value) {
    return set(target).apply(value);
  }

  public Function1<B, T> set(S target) {
    return setter.apply(target);
  }

  public Function1<S, T> modify(Function1<A, B> mapper) {
    return target -> set(target).apply(mapper.apply(getter.apply(target)));
  }

  public Function1<S, T> modify(B newValue) {
    return modify(cons(newValue));
  }

  public POptional<S, T, A, B> asOptional() {
    return POptional.of(this::set, getter.andThen(Either::right));
  }

  public <C, D> PLens<S, T, C, D> compose(PLens<A, B, C, D> other) {
    return new PLens<>(
        target -> other.get(this.get(target)),
        target -> value -> this.set(target).apply(other.modify(value).apply(this.get(target))));
  }

  public <C, D> POptional<S, T, C, D> compose(POptional<A, B, C, D> other) {
    return asOptional().compose(other);
  }

  public <C, D> PLens<S, T, C, D> compose(PIso<A, B, C, D> other) {
    return compose(other.asLens());
  }

  public <C, D> POptional<S, T, C, D> compose(PPrism<A, B, C, D> other) {
    return asOptional().compose(other.asOptional());
  }
}
