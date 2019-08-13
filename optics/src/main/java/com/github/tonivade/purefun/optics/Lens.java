/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.optics;

import static java.util.Objects.requireNonNull;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Function2;
import com.github.tonivade.purefun.Operator1;

public final class Lens<S, A> {

  private final Function1<S, A> getter;
  private final Function1<S, Function1<A, S>> setter;

  private Lens(Function1<S, A> getter, Function1<S, Function1<A, S>> setter) {
    this.getter = requireNonNull(getter);
    this.setter = requireNonNull(setter);
  }

  public static <S, A> Lens<S, A> of(Function1<S, A> getter, Function2<S, A, S> setter) {
    return new Lens<>(getter, setter.curried());
  }

  public A get(S target) {
    return getter.apply(target);
  }

  public S set(S target, A value) {
    return set(target).apply(value);
  }

  public Function1<A, S> set(S target) {
    return setter.apply(target);
  }

  public Operator1<S> modify(Operator1<A> mapper) {
    return target -> set(target).apply(mapper.apply(getter.apply(target)));
  }

  public Operator1<S> modify(A newValue) {
    return modify(ignore -> newValue);
  }

  public <B> Lens<S, B> compose(Lens<A, B> other) {
    return new Lens<>(
        target -> other.get(this.get(target)),
        target -> value -> this.set(target).apply(other.modify(value).apply(this.get(target))));
  }
}
